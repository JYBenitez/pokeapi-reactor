package skaro.trainer.api;

// Forma de wire compartida por IVs y EVs (mismas 6 estadísticas) — el
// dominio los modela como dos tipos distintos (Ivs/Evs) porque tienen
// reglas de negocio distintas (BR-002 vs BR-003/004), pero el JSON es
// idéntico.
public record StatBlockPayload(int hp, int attack, int defense, int specialAttack, int specialDefense, int speed) {
}
