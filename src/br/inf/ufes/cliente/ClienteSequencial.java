package br.inf.ufes.cliente;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import br.inf.ufes.mestre.MasterImpl;
import br.inf.ufes.ppd.Guess;
import br.inf.ufes.ppd.Master;
import br.inf.ufes.ppd.Slave;

public class ClienteSequencial 
{
	// Cores usadas para impressão no terminal
	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_VERMELHO = "\u001B[31m";
	public static final String ANSI_VERDE = "\u001B[32m";
	public static final String ANSI_AMARELO = "\u001B[33m";
	public static final String ANSI_AZUL = "\u001B[34m";
	
	private static byte[] palavraConhecida;
	private static List<String> dicionario = new ArrayList<String>();
	
	private static void lerDicionario()
	{
		try 
		{
			FileReader arq = new FileReader("../dictionary.txt");
			BufferedReader lerArq = new BufferedReader(arq);
 
			// lê a primeira linha
			// a variável "linha" recebe o valor "null" quando o processo
			// de repetição atingir o final do arquivo texto
			String linha = lerArq.readLine(); 
			
			// Adiciona a palavra no dicionario
			dicionario.add(linha);
			
			while (linha != null) 
			{
				linha = lerArq.readLine(); // lê da segunda até a última linha
				
				if(linha != null)
				{
					dicionario.add(linha);
				}
			}
 
			arq.close();
		} 
		catch (IOException e) 
		{
			System.err.printf("Erro na abertura do arquivo: %s.\n",e.getMessage());
		}
	}
	
	private static void setPalavraConhecida(byte[] palavra)
	{
		palavraConhecida = palavra;
	}
	
	// Verifica se uma sequência de bytes existe dentro de outra
	public static boolean bytesContains(byte[] mensagem, byte[] knowtext) 
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
		
	private static byte[] gerarMensagem(int tamanhoVetorGerado)
	{
		byte[] mensagem = null;
		Random numeroAleatorio = new Random();
		
		try
		{
			//Cria um vetor de bytes aleatório
			mensagem = new byte[tamanhoVetorGerado];
	        new Random().nextBytes(mensagem);
	        
	        //Armazena um trecho conhecido do vetor de bytes aleatório
	        //A faixa pode ser alterada
	        byte[] palavra = Arrays.copyOfRange(mensagem, 0,5);
	        
	        //Salva o trecho conhecido
	        setPalavraConhecida(palavra);
	        
	        //Criptografa o vetor de bytes aleatório usando uma palavra aleatória do dicionário
	        byte[] key = dicionario.get(numeroAleatorio.nextInt(dicionario.size())).getBytes();
	        
			SecretKeySpec keySpec = new SecretKeySpec(key, "Blowfish");
	
			Cipher cipher = Cipher.getInstance("Blowfish");
			cipher.init(Cipher.ENCRYPT_MODE, keySpec);
	
			mensagem = cipher.doFinal(mensagem);
			
		}
		catch (Exception e) 
		{	
			System.err.printf("Erro no método gerarMensagem: %s.\n",e.getMessage());
		}	
		
		return mensagem;
	}
	
	public static List<Guess> atacar(byte[] ciphertext, byte[] knowntext)
	{
		//Armazena os guesses
		List<Guess> g = new ArrayList<Guess>();
		
		try 
		{	
			// Percorre o dicionário
			for(String p : dicionario) 
			{
				// Pega cada palavra candidata
				String palavra = p;
				byte[] decrypted = null;
				
				try
				{
					// Usa a palavra para descriptografar o ciphertext
					byte[] key = palavra.getBytes();
					SecretKeySpec keySpec = new SecretKeySpec(key, "Blowfish");
					Cipher cipher = Cipher.getInstance("Blowfish");
					cipher.init(Cipher.DECRYPT_MODE, keySpec);
					decrypted = cipher.doFinal(ciphertext);
				} 
				catch (javax.crypto.BadPaddingException e) 
				{	
					continue;
				}
				
				// Verifica se o knowntext existe no texto descriptografado
				if(bytesContains(decrypted, knowntext))
				{
					// Armazena o guess na lista
					Guess currentguess = new Guess();
					currentguess.setKey(palavra);
					currentguess.setMessage(decrypted);
					g.add(currentguess);
				}
			}
		} 
		catch (Exception e) 
		{
			e.getMessage();
		}
		
		return g;
	}
	
	public Guess[] attack(byte[] ciphertext, byte[] knowntext) throws RemoteException 
	{		
		// Converte a lista de guesses em um array
		List<Guess> listaGuess = atacar(ciphertext, knowntext);
		Guess[] g = listaGuess.toArray(new Guess[listaGuess.size()]);;
		
		// Retorna os guesses
		return g;
	}
	
	public static void main(String[] args) throws Exception 
	{	
		// Se os argumentos não foram fornecidos
		if(args.length < 3)
		{
			throw new Exception("Uso: ClienteSequencial <NÚMERO_DE_ATAQUES> <TAMANHO_VETOR_INICIAL> <INTERVALO_VETORES>");
		}
		
		//Captura os parâmetros
		int numeroDeAtaques = Integer.parseInt(args[0]);
		int tamanhoVetorGerado = Integer.parseInt(args[1]); 
		int intervaloVetor = Integer.parseInt(args[2]);
		
		
		byte[] mensagem;
		Guess[] guess;
		
		try
		{	
			//Cria o arquivo para salvar os tempos
			PrintStream write = new PrintStream("ClienteSequencial.csv"); 
			
			ClienteSequencial clienteSequencial = new ClienteSequencial();
					
			write.print(";ClienteSequencial\n");
			
			//Armazena as palavras do dicionário
			lerDicionario();
			
			//Chama os ataques de acordo com o número de ataques passado na linha de comando
			for(int i=0; i<numeroDeAtaques; i++)
			{
				// Somatorio dos tempos (5 execuções)
				long somatorioTempos = 0;
				
				//Gera um arquivo aleatório
				mensagem = gerarMensagem(tamanhoVetorGerado);
				
				// Faz 5 vezes, para tirar a média
				for(int j=0; j<5; j++)
				{
					//Tempo inicial
					long inicio = System.nanoTime();
					
					//Chama um ataque
					guess = clienteSequencial.attack(mensagem, palavraConhecida);
					
					//Salvar o tempo (em milissegundos)
					somatorioTempos += System.nanoTime()-inicio;
				}
				
				//Salvar o tempo (em milissegundos)
				write.print(tamanhoVetorGerado+";="+(somatorioTempos/5)+"/1000000\n");
				
				//Aumenta o tamanho do vetor
				tamanhoVetorGerado += intervaloVetor;
				
				//Imprime uma mensagem para o cliente quando um ataque termina
				System.err.println(ANSI_AZUL+"[ATAQUE "+(i+1)+"] FINALIZADO!"+ANSI_RESET);
			}
			
			//Imprime uma mensagem de sucesso
			System.err.println(ANSI_VERDE+"\n"+numeroDeAtaques+" ATAQUE(S) FINALIZADO(S) COM SUCESSO!"+ANSI_RESET);
			
		    write.close();
		}
		catch (Exception e) 
		{
			System.err.printf("Erro no método main: %s.\n",e.getMessage());
		}
	}	
}
