package thearchis.common.card;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import thearchis.common.game.card.CardRarity;

class CardRarityTest {

	@Test
	@DisplayName("Verifica i costi di craft e i valori di disenchant per ogni rarità")
	void testRarityValues() {
		assertEquals(40, CardRarity.COMMON.getCraftCost());
		assertEquals(5, CardRarity.COMMON.getDisenchantValue());

		assertEquals(100, CardRarity.RARE.getCraftCost());
		assertEquals(20, CardRarity.RARE.getDisenchantValue());

		assertEquals(400, CardRarity.EPIC.getCraftCost());
		assertEquals(100, CardRarity.EPIC.getDisenchantValue());

		assertEquals(1600, CardRarity.LEGENDARY.getCraftCost());
		assertEquals(400, CardRarity.LEGENDARY.getDisenchantValue());
	}

}