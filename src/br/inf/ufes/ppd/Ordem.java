package br.inf.ufes.ppd;

import java.io.Serializable;

public class Ordem implements Serializable 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int attackNumber;
	private int indiceInicial;
	private int indiceFinal;
	private byte[] ciphertext;
	private byte[] knowntext;
	
	public Ordem(int attackNumber, int indiceInicial, int indiceFinal, byte[] ciphertext, byte[] knowntext) {
		this.attackNumber = attackNumber;
		this.indiceInicial = indiceInicial;
		this.indiceFinal = indiceFinal;
		this.ciphertext = ciphertext;
		this.knowntext = knowntext;
	}
	
	public int getAttackNumber() {
		return attackNumber;
	}
	public void setAttackNumber(int attackNumber) {
		this.attackNumber = attackNumber;
	}
	public int getIndiceInicial() {
		return indiceInicial;
	}
	public void setIndiceInicial(int indiceInicial) {
		this.indiceInicial = indiceInicial;
	}
	public int getIndiceFinal() {
		return indiceFinal;
	}
	public void setIndiceFinal(int indiceFinal) {
		this.indiceFinal = indiceFinal;
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
