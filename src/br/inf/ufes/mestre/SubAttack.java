package br.inf.ufes.mestre;

import java.rmi.RemoteException;
import java.util.Timer;
import java.util.TimerTask;

import br.inf.ufes.ppd.SlaveManager;

public class SubAttack {

	private long horaInicio = System.nanoTime();
	private long horaUltimoCheckpoint;
	private int attackNumber;
	private int subAttackNumber;
	private long currentindex;
	private long finalindex;
	java.util.UUID slaveKey;
	MasterImpl mestre;
	final Timer t;
	
	public SubAttack(int subAttackNumber, int attackNumber, long initialIndex, long finalindex, 
			java.util.UUID slaveKey, SlaveManager m) {
		this.subAttackNumber = subAttackNumber;
		this.attackNumber = attackNumber;
		this.finalindex = finalindex;
		this.currentindex = initialIndex - 1;
		this.mestre = (MasterImpl) m;
		this.slaveKey = slaveKey;
		this.horaUltimoCheckpoint = System.nanoTime();
		
		// Executa e agenda a execução de monitorarSubattack a cada 20 seg
		t = new Timer();
		t.schedule(
			new TimerTask() 
		    {
		    	@Override
		        public void run() 
		    	{
		    		try 
		    		{
		    			monitorarSubattack();
		    		} 
		    		catch(RemoteException e) 
		    		{
		    			e.printStackTrace();
		    		}
		        }
		    }, 
		0, 20000);
	}
	
	private void monitorarSubattack() throws RemoteException 
	{
		// Calcula tempo desde o último checkpoint
		long tempoDesdeOultimoCheckpoint = System.nanoTime() - horaUltimoCheckpoint;

		// Se passou 20s desde o ultimo checkpoint
		if(tempoDesdeOultimoCheckpoint > 20000000000L)
		{
			// Chama função do mestre para remover escravo
			//mestre.removeSlave(slaveKey);
						
			// Distribui este subattack para outros escravos
			//mestre.redistribuirSubAttack(subAttackNumber);
			
			// Para de monitorar este sub-ataque
			pararMonitoramento();
		}
	}
	
	protected void pararMonitoramento() {
		t.cancel();
	}

	public long getHoraInicio() {
		return horaInicio;
	}

	public int getSubAttackNumber() {
		return subAttackNumber;
	}

	public long getCurrentindex() {
		return currentindex;
	}

	public void setCurrentindex(long currentindex) 
	{
		this.currentindex = currentindex;
		this.horaUltimoCheckpoint = System.nanoTime();
		
		// Se for o último índice
		if(this.currentindex == this.finalindex) 
		{
			// Encerra o sub-ataque no mestre
			//mestre.encerrarSubAttack(subAttackNumber, attackNumber);
			
			// Para de monitorar este sub-ataque
			pararMonitoramento();
		}
	}

	public int getAttackNumber() {
		return attackNumber;
	}

	public long getFinalindex() {
		return finalindex;
	}
}
