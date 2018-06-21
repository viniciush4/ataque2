package br.inf.ufes.mestre;

import java.util.ArrayList;

import br.inf.ufes.ppd.Guess;

public class Attack {
	
	private int attackNumber;
	private int quantidadeSubataquesEmAndamento;
	private byte[] ciphertext;
	private byte[] knowntext;
	ArrayList<Guess> guesses = new ArrayList<Guess>();
	
	public Attack(int attackNumber, byte[] ciphertext, byte[] knowntext) 
	{
		this.attackNumber = attackNumber;
		this.setCiphertext(ciphertext);
		this.setKnowntext(knowntext);
	}
	
	public int getAttackNumber() {
		return attackNumber;
	}

	public int getQuantidadeSubataquesEmAndamento() {
		return quantidadeSubataquesEmAndamento;
	}

	public void incrementaSubataquesEmAndamento() {
		this.quantidadeSubataquesEmAndamento++;
	}
	
	public void decrementaSubataquesEmAndamento() {
		this.quantidadeSubataquesEmAndamento--;
	}

	public byte[] getCiphertext() {
		return ciphertext;
	}

	public void setCiphertext(byte[] ciphertext) {
		this.ciphertext = ciphertext;
	}

	public byte[] getKnowntext() {
		return knowntext;
	}

	public void setKnowntext(byte[] knowntext) {
		this.knowntext = knowntext;
	}
}
