package br.inf.ufes.escravo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.jms.*;
import javax.naming.NamingException;
import com.sun.messaging.ConnectionConfiguration;
import br.inf.ufes.ppd.Guess;
import br.inf.ufes.ppd.Ordem;

public class SlaveImpl implements MessageListener 
{
	// Modo Overhead
	private static boolean overhead;
	
	// Host
	private static String host;
		
	// Nome do escravo
	private static String nomeEscravo;

	// Fila de sub-ataques
	private static Queue subAttacks;
	private static JMSProducer producer;
	
	// Fila de chutes
	private static Queue guesses;
	private static JMSConsumer consumer;
	
	// Contexto JMS
	private static JMSContext context;
	
	// Dicionário
	private static String[] dicionario;
	
	public static void main(String[] args)
	{
		try 
		{
			// Se não foi fornecido exatamente um argumento, lança uma exceção
			if(args.length < 2) { throw new Exception("Uso: SlaveImpl <IP_DO_MESTRE> <NOME_ESCRAVO> <HAB_MODO_OVERHEAD? 0-N | 1-S>");}
			
			// Recebe o host
			host = args[0];

			// Recebe o nome do escravo
			nomeEscravo = args[1];
			
			// Guarda preferência do modo overhead
			overhead = (Integer.parseInt(args[2]) == 1) ? true : false;
			
			// Configura JMS
			configurarJMS();
			
			// Lê o dicionário
			lerDicionario();
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
	
	public static void configurarJMS() throws JMSException, NamingException 
	{
		// Cria e configura a conection factory
		Logger.getLogger("").setLevel(Level.SEVERE);
		com.sun.messaging.ConnectionFactory connectionFactory = new com.sun.messaging.ConnectionFactory();
		connectionFactory.setProperty(ConnectionConfiguration.imqAddressList,host+":7676");
		connectionFactory.setProperty(ConnectionConfiguration.imqConsumerFlowLimitPrefetch,"false");
		System.out.println("["+nomeEscravo+"] Resolved connection factory.");

		// Conecta com as filas de sub-ataques e guesses
		subAttacks = new com.sun.messaging.Queue("SubAttacksQueue");
		guesses = new com.sun.messaging.Queue("GuessesQueue");
		System.out.println("["+nomeEscravo+"] Resolved queue.");

		// Cria context, producer e consumer
		context = connectionFactory.createContext();
		producer = context.createProducer();
		consumer = context.createConsumer(subAttacks,null,false); 
		
		// Define a classe como ouvidor de mensagens
		consumer.setMessageListener(new SlaveImpl()); 
	}
	
	public static void lerDicionario()
	{
		try 
		{
			int tamanhoDicionario = 80368;
			dicionario = new String[tamanhoDicionario];
			
			File arquivo = new File("../dictionary.txt");
			FileReader arq = new FileReader(arquivo);
			BufferedReader lerArq = new BufferedReader(arq);
			
			// Mantém as palavras do dicionario em memória
			for(int i=0; i<tamanhoDicionario; i++) 
			{
				dicionario[i] = lerArq.readLine();
			}
		
			// Fecha o arquivo do dicionario
			arq.close();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}

	@Override
	public void onMessage(Message m) 
	{
		try 
		{
			if (m instanceof ObjectMessage)
			{
				Ordem ordem = (Ordem)((ObjectMessage)m).getObject();
				startSubAttack(ordem);
			}
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
	
	public void startSubAttack(Ordem ordem)
	{
		// Se não estiver em modo overhead
		if(!overhead) 
		{
			// Imprime os índices inicial e final do sub-ataque
			System.out.println("["+nomeEscravo+" #"+ordem.getAttackNumber()+"] índices "+ordem.getIndiceInicial()+" a "+ordem.getIndiceFinal());
			
			// Percorre o intervalo solicitado no dicionario
			for(int i=ordem.getIndiceInicial(); i<=ordem.getIndiceFinal();i++) 
			{
				// Busca a palavra no dicionário
				String palavra = dicionario[i];
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
				catch (javax.crypto.BadPaddingException | 
						NoSuchPaddingException | 
						NoSuchAlgorithmException | 
						InvalidKeyException | 
						IllegalBlockSizeException e) 
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
					currentguess.setNomeEscravo(nomeEscravo);
					ObjectMessage message = context.createObjectMessage(currentguess);
					producer.send(guesses,message);
					
					// Imprime no escravo os índices inicial e final
					System.err.println("["+nomeEscravo+" #"+ordem.getAttackNumber()+"] "+i+" "+currentguess.getKey());
				}
			}
		}
		
		// Imprime no escravo aviso de fim
		System.err.println("["+nomeEscravo+" #"+ordem.getAttackNumber()+"] Sub-ataque finalizado");
		
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
