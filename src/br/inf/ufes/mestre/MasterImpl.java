package br.inf.ufes.mestre;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.*;
import javax.naming.NamingException;
import com.sun.messaging.ConnectionConfiguration;
import br.inf.ufes.ppd.Guess;
import br.inf.ufes.ppd.Master;
import br.inf.ufes.ppd.Ordem;

public class MasterImpl implements Master
{
	// Quantidade de índices por sub-ataque
	private static int m;
	
	// Host
	private static String host;
	
	// Fila de sub-ataques
	private static Queue subAttacks;
	private static JMSProducer producer;
	
	// Fila de chutes
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
			// Se não foi fornecido exatamente um argumento, lança exceção
			if(args.length < 2) { throw new Exception("Uso: MasterImpl <IP_DESTA_MÁQUINA> <M>"); }

			// Recebe o host e a quantidade de índices por sub-ataque
			host = args[0];
			m = Integer.parseInt(args[1]);
			
			// Configura RMI e JMS
			configurarRMI();
			configurarJMS();
			
			while(true) 
			{
				Message m = consumer.receive();
				if (m instanceof ObjectMessage)
				{
					// Objeto guess recebido
					Guess guess = (Guess)((ObjectMessage) m).getObject();
					
					// Coloca o guess na lista do ataque correspondente
					synchronized(attacks) {attacks.get(guess.getAttackNumber()).guesses.add(guess);}
					
					// Imprime aviso de chegada de guess no mestre
					System.out.println("["+guess.getNomeEscravo()+" #"+guess.getAttackNumber()+"] "+guess.getKey());
				}
				if (m instanceof TextMessage)
				{
					// Coverte a mensagem em um interio (representa o número do ataque)
					int attackNumber = Integer.parseInt(((TextMessage) m).getText());
					int quantidadeSubAttacksEmAndamento;
					
					synchronized(attacks) 
					{
						// Decrementa a quantidade de sub-ataques em andamento
						attacks.get(attackNumber).decrementaSubataquesEmAndamento();
						
						quantidadeSubAttacksEmAndamento = attacks.get(attackNumber).getQuantidadeSubataquesEmAndamento();
					}
						
					// Se a quantidade de sub-ataques em andamento chegou a zero
					if(quantidadeSubAttacksEmAndamento == 0) 
					{
						// Acorda a thread do mestre
						synchronized(MasterImpl.class) {MasterImpl.class.notify();}
					}
				}
			}
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
		// Cria e configura a conection factory
		Logger.getLogger("").setLevel(Level.SEVERE);
		com.sun.messaging.ConnectionFactory connectionFactory = new com.sun.messaging.ConnectionFactory();
		connectionFactory.setProperty(ConnectionConfiguration.imqAddressList,host+":7676");	
		System.out.println("[master] Resolved connection factory.");

		// Conecta com as filas de sub-ataques e guesses
		subAttacks = new com.sun.messaging.Queue("SubAttacksQueue");
		guesses = new com.sun.messaging.Queue("GuessesQueue");
		System.out.println("[master] Resolved queue.");

		// Cria context, producer e consumer
		context = connectionFactory.createContext();
		producer = context.createProducer();
		consumer = context.createConsumer(guesses,null,false);  
	}

	public Guess[] attack(byte[] ciphertext, byte[] knowntext) throws RemoteException 
	{
		// Cria um ataque
		Attack attack = new Attack(lastAttackNumber++, ciphertext, knowntext);
		
		// Adiciona o ataque na lista de ataques
		synchronized(attacks) {	attacks.put(attack.getAttackNumber(), attack); }
		
		// Distribuição dos índices
		int tamanhoDicionario = 80368;
		int divisao = (tamanhoDicionario / m);
		int mod = tamanhoDicionario % m;
		int indiceInicial=0;
		int indiceFinal=0;
		
		// Distribui a parte interia da divisão
		for(int i=0; i<divisao; i++)
		{
			indiceInicial = i*m;
			indiceFinal = indiceInicial+m-1;
			
			Ordem ordem = new Ordem(attack.getAttackNumber(), indiceInicial, indiceFinal, ciphertext, knowntext);
			ObjectMessage message = context.createObjectMessage(ordem); 
			producer.send(subAttacks,message);
			synchronized(attacks) { attacks.get(attack.getAttackNumber()).incrementaSubataquesEmAndamento(); }
		}
		
		// Distrubui o resto da divisão
		Ordem ordem = new Ordem(attack.getAttackNumber(), (indiceFinal+1), (mod-1), ciphertext, knowntext);
		ObjectMessage message = context.createObjectMessage(ordem); 
		producer.send(subAttacks,message);
		synchronized(attacks) { attacks.get(attack.getAttackNumber()).incrementaSubataquesEmAndamento(); }
		
		try 
		{
			// Dorme até que todos os sub-ataques tenham terminado
			synchronized(MasterImpl.class) {MasterImpl.class.wait();}
		} 
		catch (InterruptedException e) 
		{
			e.printStackTrace();
		}
		
		// Converte a lista de guesses em um array
		Guess[] guesses = new Guess[attacks.get(attack.getAttackNumber()).guesses.size()];
		attacks.get(attack.getAttackNumber()).guesses.toArray(guesses);
		
		// Remove attack da lista
		synchronized(attacks) { attacks.remove(attack.getAttackNumber()); }
		
		// Retorna os guesses
		return guesses;
	}
}
