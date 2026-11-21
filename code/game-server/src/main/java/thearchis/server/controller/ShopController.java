package thearchis.server.controller;

import thearchis.server.db.ShopRepository;

public class ShopController {

	private final ShopRepository shopRepository;

	public ShopController(ShopRepository shopRepository) {
		this.shopRepository = shopRepository;
	}

}
