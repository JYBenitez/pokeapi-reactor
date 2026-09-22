package skaro.trainer.domain;

import jakarta.persistence.Embeddable;

@Embeddable
public class Evs {

	private int hp;
	private int attack;
	private int defense;
	private int specialAttack;
	private int specialDefense;
	private int speed;

	protected Evs() {
	}

	public Evs(int hp, int attack, int defense, int specialAttack, int specialDefense, int speed) {
		this.hp = hp;
		this.attack = attack;
		this.defense = defense;
		this.specialAttack = specialAttack;
		this.specialDefense = specialDefense;
		this.speed = speed;
	}

	public int getHp() {
		return hp;
	}

	public int getAttack() {
		return attack;
	}

	public int getDefense() {
		return defense;
	}

	public int getSpecialAttack() {
		return specialAttack;
	}

	public int getSpecialDefense() {
		return specialDefense;
	}

	public int getSpeed() {
		return speed;
	}

	public int total() {
		return hp + attack + defense + specialAttack + specialDefense + speed;
	}

}
