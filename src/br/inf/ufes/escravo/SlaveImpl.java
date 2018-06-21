package br.inf.ufes.escravo;

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

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.sun.messaging.ConnectionConfiguration;

import br.inf.ufes.mestre.MasterImpl;
import br.inf.ufes.ppd.Guess;
import br.inf.ufes.ppd.Master;
import br.inf.ufes.ppd.Slave;

public class SlaveImpl 
{

	// Lista de sub-ataques
	private static Queue subAttacks;
	private static JMSProducer producer;
	
	// Lista de chutes recebidos
	private static Queue guesses;
	private static JMSConsumer consumer;
	
	public static void main(String[] args)
	{
		try {
			Logger.getLogger("").setLevel(Level.SEVERE);
			
			Hashtable env = new Hashtable();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.enterprise.naming.SerialInitContextFactory");
			
			InitialContext ic = new InitialContext(env);
						
			ConnectionFactory connectionFactory = (ConnectionFactory)ic.lookup("jms/__defaultConnectionFactory");
			
			System.out.println("resolved connection factory.");
			
			subAttacks = (Queue)ic.lookup("jms/SubAttacksQueue");
			guesses = (Queue)ic.lookup("jms/GuessesQueue");

			System.out.println("resolved queue.");

			JMSContext context = connectionFactory.createContext();
			
			JMSProducer producer = context.createProducer();
			JMSConsumer consumer = context.createConsumer(subAttacks,null,false); 
			MessageListener chat = new MasterImpl();
			consumer.setMessageListener(chat); 

			Scanner s = new Scanner(System.in);
			while (true)
			{
				System.out.print("enter your message:");
				String content = s.nextLine();		    
				TextMessage message = context.createTextMessage(); 
				message.setText(content);
				producer.send(guesses,message);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
