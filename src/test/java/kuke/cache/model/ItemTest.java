package kuke.cache.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ItemTest {
	@Test
	void create() {
		Item item1 = Item.create(new ItemCreateRequest("data1"));
		Item item2 = Item.create(new ItemCreateRequest("data2"));
		Item item3 = Item.create(new ItemCreateRequest("data3"));

		System.out.println("item1 = " + item1);
		System.out.println("item2 = " + item2);
		System.out.println("item3 = " + item3);

		assertThat(item1.getItemId()).isLessThan(item2.getItemId());
		assertThat(item2.getItemId()).isLessThan(item3.getItemId());
	}

	@Test
	void update() {
		Item item = Item.create(new ItemCreateRequest("data1"));
		item.update(new ItemUpdateRequest("data2"));
		assertThat(item.getData()).isEqualTo("data2");
	}
}