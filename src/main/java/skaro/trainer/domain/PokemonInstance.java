package skaro.trainer.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

// Ejemplar propio de un entrenador. No se llama "Pokemon" para no
// colisionar con skaro.pokeapi.resource.Pokemon (el DTO estático de
// especie) — son conceptos distintos, ver "Alcance del cambio" de la spec.
@Entity
public class PokemonInstance {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "trainer_id")
	private Trainer trainer;

	private String species;

	@Embedded
	@AttributeOverrides({
			@AttributeOverride(name = "hp", column = @Column(name = "iv_hp")),
			@AttributeOverride(name = "attack", column = @Column(name = "iv_attack")),
			@AttributeOverride(name = "defense", column = @Column(name = "iv_defense")),
			@AttributeOverride(name = "specialAttack", column = @Column(name = "iv_special_attack")),
			@AttributeOverride(name = "specialDefense", column = @Column(name = "iv_special_defense")),
			@AttributeOverride(name = "speed", column = @Column(name = "iv_speed")) })
	private Ivs ivs;

	@Embedded
	@AttributeOverrides({
			@AttributeOverride(name = "hp", column = @Column(name = "ev_hp")),
			@AttributeOverride(name = "attack", column = @Column(name = "ev_attack")),
			@AttributeOverride(name = "defense", column = @Column(name = "ev_defense")),
			@AttributeOverride(name = "specialAttack", column = @Column(name = "ev_special_attack")),
			@AttributeOverride(name = "specialDefense", column = @Column(name = "ev_special_defense")),
			@AttributeOverride(name = "speed", column = @Column(name = "ev_speed")) })
	private Evs evs;

	@Enumerated(EnumType.STRING)
	private Nature nature;

	private String ability;

	private boolean shiny;

	@Enumerated(EnumType.STRING)
	private Gender gender;

	// EAGER a propósito: colección acotada (máx. 4, BR-008) que siempre se
	// necesita completa en la respuesta — evita LazyInitializationException
	// una vez que la entidad sale de la sesión JPA hacia domain/api.
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "pokemon_instance_moves", joinColumns = @JoinColumn(name = "pokemon_instance_id"))
	@Column(name = "move_name")
	private List<String> moves = new ArrayList<>();

	private String heldItem;

	private String pokeball;

	private Instant captureDate;

	private int initialLevel;

	private String originLocation;

	@Enumerated(EnumType.STRING)
	private Location location;

	protected PokemonInstance() {
	}

	public PokemonInstance(Trainer trainer, String species) {
		this.trainer = trainer;
		this.species = species;
	}

	public Long getId() {
		return id;
	}

	public Trainer getTrainer() {
		return trainer;
	}

	public String getSpecies() {
		return species;
	}

	public Ivs getIvs() {
		return ivs;
	}

	public void setIvs(Ivs ivs) {
		this.ivs = ivs;
	}

	public Evs getEvs() {
		return evs;
	}

	public void setEvs(Evs evs) {
		this.evs = evs;
	}

	public Nature getNature() {
		return nature;
	}

	public void setNature(Nature nature) {
		this.nature = nature;
	}

	public String getAbility() {
		return ability;
	}

	public void setAbility(String ability) {
		this.ability = ability;
	}

	public boolean isShiny() {
		return shiny;
	}

	public void setShiny(boolean shiny) {
		this.shiny = shiny;
	}

	public Gender getGender() {
		return gender;
	}

	public void setGender(Gender gender) {
		this.gender = gender;
	}

	public List<String> getMoves() {
		return moves;
	}

	public void setMoves(List<String> moves) {
		this.moves = moves;
	}

	public String getHeldItem() {
		return heldItem;
	}

	public void setHeldItem(String heldItem) {
		this.heldItem = heldItem;
	}

	public String getPokeball() {
		return pokeball;
	}

	public void setPokeball(String pokeball) {
		this.pokeball = pokeball;
	}

	public Instant getCaptureDate() {
		return captureDate;
	}

	public void setCaptureDate(Instant captureDate) {
		this.captureDate = captureDate;
	}

	public int getInitialLevel() {
		return initialLevel;
	}

	public void setInitialLevel(int initialLevel) {
		this.initialLevel = initialLevel;
	}

	public String getOriginLocation() {
		return originLocation;
	}

	public void setOriginLocation(String originLocation) {
		this.originLocation = originLocation;
	}

	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

}
