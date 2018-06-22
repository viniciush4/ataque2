package br.inf.ufes.ppd;



/**
 * Guess.java
 */


import java.io.Serializable;

public class Guess implements Serializable {
	
	private int attackNumber;
	
	private String key;
	// chave candidata

	private byte[] message;
	// mensagem decriptografada com a chave candidata

	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public byte[] getMessage() {
		return message;
	}
	public void setMessage(byte[] message) {
		this.message = message;
	}
	public int getAttackNumber() {
		return attackNumber;
	}
	public void setAttackNumber(int attackNumber) {
		this.attackNumber = attackNumber;
	}

}
