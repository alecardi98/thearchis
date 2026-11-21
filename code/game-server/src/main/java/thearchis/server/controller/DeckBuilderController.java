package thearchis.server.controller;

import thearchis.server.db.DeckBuilderRepository;

public class DeckBuilderController {

	private final DeckBuilderRepository deckBuilderRepository;

	public DeckBuilderController(DeckBuilderRepository deckBuilderRepository) {
		this.deckBuilderRepository = deckBuilderRepository;
	}

}
