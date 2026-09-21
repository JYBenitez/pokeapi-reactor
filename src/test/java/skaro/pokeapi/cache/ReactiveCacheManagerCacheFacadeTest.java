package skaro.pokeapi.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;
import reactor.test.StepVerifier;
import skaro.pokeapi.resource.pokemon.Pokemon;

@ExtendWith(SpringExtension.class)
public class ReactiveCacheManagerCacheFacadeTest {

	@Mock
	private CacheManager cacheManager;
	@Mock
	private Cache cache;
	
	private ReactiveCacheManagerCacheFacade facade;
	
	@BeforeEach
	public void setup() {
		this.facade = new ReactiveCacheManagerCacheFacade(cacheManager);
	}
	
	// Spec 000 — Escenario: cache miss resuelve con el proveedor y cachea el resultado
	@Test
	@SuppressWarnings("unchecked")
	public void getTest_cacheMiss() {
		String key = UUID.randomUUID().toString();
		Pokemon value = new Pokemon();
		ArgumentCaptor<Signal<Pokemon>> cachedValueCaptor = ArgumentCaptor.forClass(Signal.class);
		
		when(cacheManager.getCache(value.getClass().getName()))
			.thenReturn(cache);
		when(cache.get(key))
			.thenReturn(new SimpleValueWrapper(null));
		
		CacheSpec<Pokemon> cacheSpec = CacheSpec.get(Pokemon.class, key)
				.orCache(() -> Mono.just(value));
		
		StepVerifier.create(facade.get(cacheSpec))
			.expectNext(value)
			.expectComplete()
			.verify();
	
		verify(cache).put(eq(key), cachedValueCaptor.capture());
		
		Pokemon cachedValue = cachedValueCaptor.getValue().get();
		assertEquals(value, cachedValue);
	}
	
	// Spec 000 — Escenario: cache hit devuelve el valor cacheado sin llamar al proveedor original
	@Test
	public void getTest_cacheHit() {
		String key = UUID.randomUUID().toString();
		Pokemon value = new Pokemon();

		when(cacheManager.getCache(value.getClass().getName()))
			.thenReturn(cache);
		when(cache.get(key))
			.thenReturn(new SimpleValueWrapper(Signal.next(value)));
		
		CacheSpec<Pokemon> cacheSpec = CacheSpec.get(Pokemon.class, key)
				.orCache(() -> Mono.empty());
		
		StepVerifier.create(facade.get(cacheSpec))
			.expectNext(value)
			.expectComplete()
			.verify();
		
		verify(cache, never()).put(any(), any());
	}
	
	// Spec 000 — Escenario: cache nombrado inexistente resuelve igual contra el proveedor
	// sin cachear, logueando ERROR
	@Test
	public void getTest_cacheDoesNotExist() {
		String key = UUID.randomUUID().toString();
		Pokemon value = new Pokemon();

		when(cacheManager.getCache(value.getClass().getName()))
			.thenReturn(null);

		CacheSpec<Pokemon> cacheSpec = CacheSpec.get(Pokemon.class, key)
				.orCache(() -> Mono.just(value));

		Logger facadeLogger = (Logger) org.slf4j.LoggerFactory.getLogger(ReactiveCacheManagerCacheFacade.class);
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		facadeLogger.addAppender(appender);

		StepVerifier.create(facade.get(cacheSpec))
			.expectNext(value)
			.expectComplete()
			.verify();

		verify(cache, never()).put(any(), any());

		facadeLogger.detachAppender(appender);
		assertTrue(appender.list.stream().anyMatch(event ->
				event.getLevel() == Level.ERROR
				&& event.getFormattedMessage().contains(value.getClass().getName())));
	}
	
	// Spec 000 — Escenario: getMany resuelve múltiples CacheSpecs en paralelo (Flux.merge)
	@Test
	public void getManyTest() {
		String key1 = UUID.randomUUID().toString();
		Pokemon value1 = new Pokemon();
		String key2 = UUID.randomUUID().toString();
		Pokemon value2 = new Pokemon();
		
		when(cacheManager.getCache(Pokemon.class.getName()))
			.thenReturn(cache);
		when(cache.get(key1))
			.thenReturn(new SimpleValueWrapper(Signal.next(value1)));
		when(cache.get(key2))
			.thenReturn(new SimpleValueWrapper(Signal.next(value2)));
		
		CacheSpec<Pokemon> cacheSpec1 = CacheSpec.get(Pokemon.class, key1)
				.orCache(() -> Mono.just(value1));
		CacheSpec<Pokemon> cacheSpec2 = CacheSpec.get(Pokemon.class, key2)
				.orCache(() -> Mono.just(value1));
		
		StepVerifier.create(facade.getMany(List.of(cacheSpec1, cacheSpec2)))
			.expectNext(value1)
			.expectNext(value2)
			.expectComplete()
			.verify();
	}
	
	
}
