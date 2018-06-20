package br.inf.ufes.mestre;

import java.rmi.RemoteException;
import java.util.Hashtable;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;


import br.inf.ufes.ppd.Guess;

public class MasterImpl implements MessageListener
{
	public static void main(String args[]) 
	{	
		try {
			Logger.getLogger("").setLevel(Level.SEVERE);
			
			Hashtable env = new Hashtable();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.enterprise.naming.SerialInitContextFactory");
			
			InitialContext ic = new InitialContext(env);
						
			ConnectionFactory connectionFactory = (ConnectionFactory)ic.lookup("jms/__defaultConnectionFactory");
			
			System.out.println("resolved connection factory.");
			
			Topic topic = (Topic)ic.lookup("jms/MyTopic");

			System.out.println("resolved topic.");

			JMSContext context = connectionFactory.createContext();
			
			JMSProducer producer = context.createProducer();
			JMSConsumer consumer = context.createConsumer(topic,null,false); 
			MessageListener chat = new MasterImpl();
			consumer.setMessageListener(chat); 

			Scanner s = new Scanner(System.in);
			while (true)
			{
				System.out.print("enter your message:");
				String content = s.nextLine();		    
				TextMessage message = context.createTextMessage(); 
				message.setText(content);
				producer.send(topic,message);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Guess[] attack(byte[] ciphertext, byte[] knowntext) throws RemoteException 
	{
		// Converte a lista de guesses em um array
		Guess[] guesses = new Guess[1];
		Guess guess = new Guess();
		guess.setKey("chave");
		guess.setMessage(null);
		guesses[0] = guess;
		
		// Retorna os guesses
		return guesses;
	}

	@Override
	public void onMessage(Message m) {
		try {
			if (m instanceof TextMessage)
			{
				System.out.print("\nreceived message: ");
				System.out.println(((TextMessage)m).getText());
				System.out.print("enter your message:");
			}
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}
}
