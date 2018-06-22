package br.inf.ufes.mestre;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.*;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.sun.messaging.ConnectionConfiguration;

import br.inf.ufes.ppd.Guess;
import br.inf.ufes.ppd.Master;
import br.inf.ufes.ppd.Ordem;
import br.inf.ufes.ppd.Slave;

public class MasterImpl implements MessageListener, Master
{
	// Quantidade de índices por sub-ataque
	private static int m;
	
	// Host
	private static String host;
	
	// Lista de sub-ataques
	private static Queue subAttacks;
	private static JMSProducer producer;
	
	// Lista de chutes
	private static Queue guesses;
	private static JMSConsumer consumer;
	
	// Contexto JMS
	private static JMSContext context;
	
	// Lista de ataques em andamento
	private static Map<Integer, Attack> attacks = new HashMap<Integer, Attack>();
	
	// Número (identificador) do último ataque
	Integer lastAttackNumber = 0;
	
	public static void main(String args[]) 
	{	
		try
		{
			// Se não foi fornecido exatamente um argumento
			if(args.length < 2) {
				throw new Exception("Uso: MasterImpl <IP_DESTA_MÁQUINA> <M>");
			}

			// Recebe o host e a quantidade de índices por sub-ataque
			host = args[0];
			m = Integer.parseInt(args[1]);
			
			configurarRMI();
			configurarJMS();
			while(true) {}
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
	
	public static void configurarRMI() throws RemoteException
	{	
		// Configura o hostname
		System.setProperty("java.rmi.server.hostname", host);

		// Cria uma referência desta classe para exportação
		Master objref = (Master) UnicastRemoteObject.exportObject(new MasterImpl(), 0);
		
		// Pega referência do registry
		Registry registry = LocateRegistry.getRegistry("127.0.0.1");
		
		// Faz o bind
		registry.rebind("mestre", objref);
		
		// Informa o status do mestre
		System.err.println("[master] Ready");
	}
	
	public static void configurarJMS() throws JMSException, NamingException 
	{
		Logger.getLogger("").setLevel(Level.SEVERE);
		
		Hashtable env = new Hashtable();
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.enterprise.naming.SerialInitContextFactory");
		
		InitialContext ic = new InitialContext(env);
					
		ConnectionFactory connectionFactory = (ConnectionFactory)ic.lookup("jms/__defaultConnectionFactory");
		
		System.out.println("resolved connection factory.");
		
		subAttacks = (Queue)ic.lookup("jms/SubAttacksQueue");
		guesses = (Queue)ic.lookup("jms/GuessesQueue");

		System.out.println("resolved queue.");

		context = connectionFactory.createContext();
		
		producer = context.createProducer();
		consumer = context.createConsumer(guesses,null,false); 
		MessageListener messageListener = new MasterImpl();
		consumer.setMessageListener(messageListener); 
	}

	public Guess[] attack(byte[] ciphertext, byte[] knowntext) throws RemoteException 
	{
		// Cria um ataque
		Attack attack = new Attack(lastAttackNumber++, ciphertext, knowntext);
		
		// Adiciona o ataque na lista de ataques
		synchronized(attacks) {	attacks.put(attack.getAttackNumber(), attack); }
		
		// Calcula os índices do dicionário para o primeiro escravo
		int tamanhoDicionario = 80368;
		int divisao = (tamanhoDicionario / m);
		int mod = tamanhoDicionario % m;
		int indiceInicial=0;
		int indiceFinal=0;
		
		for(int i=0; i<divisao; i++)
		{
			indiceInicial = i*m;
			indiceFinal = indiceInicial+m-1;
			
			Ordem ordem = new Ordem(attack.getAttackNumber(), indiceInicial, indiceFinal, ciphertext, knowntext);
			ObjectMessage message = context.createObjectMessage(ordem); 
			producer.send(subAttacks,message);
			synchronized(attacks) { attacks.get(attack.getAttackNumber()).incrementaSubataquesEmAndamento(); }
		}
		
		Ordem ordem = new Ordem(attack.getAttackNumber(), (indiceFinal+1), (mod-1), ciphertext, knowntext);
		ObjectMessage message = context.createObjectMessage(ordem); 
		producer.send(subAttacks,message);
		synchronized(attacks) { attacks.get(attack.getAttackNumber()).incrementaSubataquesEmAndamento(); }
		
		// Espera até que todos os sub-ataques tenham terminado
		while(attacks.get(attack.getAttackNumber()).getQuantidadeSubataquesEmAndamento() > 0){}
		
		// Converte a lista de guesses em um array
		Guess[] guesses = new Guess[attacks.get(attack.getAttackNumber()).guesses.size()];
		attacks.get(attack.getAttackNumber()).guesses.toArray(guesses);
		
		// Remove attack da lista
		synchronized(attacks) { attacks.remove(attack.getAttackNumber()); }
		
		// Retorna os guesses
		return guesses;
	}

	@Override
	public void onMessage(Message m) 
	{
		try 
		{
			if (m instanceof ObjectMessage)
			{
				Guess guess = (Guess)((ObjectMessage) m).getObject();
				
				// Coloca o guess na lista do ataque correspondente
				synchronized(attacks) {attacks.get(guess.getAttackNumber()).guesses.add(guess);}
				
				System.out.println("[#"+guess.getAttackNumber()+"] "+guess.getKey());
			}
			if (m instanceof TextMessage)
			{
				int attackNumber = Integer.parseInt(((TextMessage) m).getText());
				
				// Decrementa a quantidade de sub-ataques em andamento
				synchronized(attacks) {attacks.get(attackNumber).decrementaSubataquesEmAndamento();}
				
				//synchronized(attacks) {System.out.println("[#"+attackNumber+"] "+attacks.get(attackNumber).getQuantidadeSubataquesEmAndamento());}
			}
		} 
		catch (JMSException e) 
		{
			e.printStackTrace();
		}
	}
}
