package br.inf.ufes.escravo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.sun.messaging.ConnectionConfiguration;

import br.inf.ufes.mestre.MasterImpl;
import br.inf.ufes.ppd.Guess;
import br.inf.ufes.ppd.Master;
import br.inf.ufes.ppd.Ordem;
import br.inf.ufes.ppd.Slave;

public class SlaveImpl implements MessageListener 
{

	// Lista de sub-ataques
	private static Queue subAttacks;
	private static JMSProducer producer;
	
	// Lista de chutes
	private static Queue guesses;
	private static JMSConsumer consumer;
	
	// Contexto JMS
	private static JMSContext context;
	
	public static void main(String[] args)
	{
		try 
		{
			configurarJMS();
			while(true) {}

		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
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
		consumer = context.createConsumer(subAttacks,null,false); 
		MessageListener messageListener = new SlaveImpl();
		consumer.setMessageListener(messageListener); 
	}

	@Override
	public void onMessage(Message m) 
	{
		try 
		{
			if (m instanceof ObjectMessage)
			{
				Ordem ordem = (Ordem)((ObjectMessage)m).getObject();
				System.out.println("[#"+ordem.getAttackNumber()+"] índices "+ordem.getIndiceInicial()+" a "+ordem.getIndiceFinal());
				startSubAttack(ordem);
			}
		} 
		catch (JMSException e) 
		{
			e.printStackTrace();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void startSubAttack(Ordem ordem)
	{
		try
		{
			// Lê o arquivo do dicionário
			File arquivo = new File("../dictionary.txt");
			FileReader arq = new FileReader(arquivo);
			BufferedReader lerArq = new BufferedReader(arq);
			
			// Avança até índice inicial
			for(long i=0;i<ordem.getIndiceInicial();i++) { lerArq.readLine(); }
			
			// Percorre o intervalo solicitado no dicionario
			for(long i=ordem.getIndiceInicial(); i<=ordem.getIndiceFinal();i++) 
			{
				// Lê a palavra candidata
				String palavra = lerArq.readLine();
				byte[] decrypted = null;
				
				try
				{
					// Usa a palavra para descriptografar o ciphertext
					byte[] key = palavra.getBytes();
					SecretKeySpec keySpec = new SecretKeySpec(key, "Blowfish");
					Cipher cipher = Cipher.getInstance("Blowfish");
					cipher.init(Cipher.DECRYPT_MODE, keySpec);
					decrypted = cipher.doFinal(ordem.getCiphertext());
				} 
				catch (javax.crypto.BadPaddingException e) 
				{
					continue;
				}
				
				// Verifica se o knowntext existe no texto descriptografado
				if(bytesContains(decrypted, ordem.getKnowntext()))
				{
					// Avisa ao mestre 
					Guess currentguess = new Guess();
					currentguess.setKey(palavra);
					currentguess.setMessage(decrypted);
					currentguess.setAttackNumber(ordem.getAttackNumber());
					ObjectMessage message = context.createObjectMessage(currentguess);
					producer.send(guesses,message);
					
					// Imprime no escravo os índices inicial e final
					System.err.println("[#"+ordem.getAttackNumber()+"] "+i+" "+currentguess.getKey());
				}
			}
			
			// Fecha o arquivo do dicionario
			arq.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		// Imprime no escravo aviso de fim
		System.err.println("[#"+ordem.getAttackNumber()+"] Sub-ataque finalizado");
		
		// Informa ao mestre o término do sub-ataque
		TextMessage message = context.createTextMessage(ordem.getAttackNumber()+"");
		producer.send(guesses,message);
	}
	
	// Verifica se uma sequência de bytes existe dentro de outra
	public boolean bytesContains(byte[] mensagem, byte[] knowtext) 
	{
		int contadorBytesIguais;
		
        for(int i = 0; i < mensagem.length; i++) {
        	contadorBytesIguais=0;
            if(mensagem[i] == knowtext[0]) {
            	contadorBytesIguais++;
                for(int j = 1, k = i+1; (j < knowtext.length) && (k < mensagem.length); j++, k++) {
                	
                    if(knowtext[j] != mensagem[k]) break;
                    contadorBytesIguais++;
                }
                if(contadorBytesIguais == knowtext.length) return true;
            }
        }
        return false;
    }
}
