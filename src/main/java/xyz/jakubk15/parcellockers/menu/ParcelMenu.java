package xyz.jakubk15.parcellockers.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.button.ButtonMenu;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;

public class ParcelMenu extends Menu {
	private int chosenPackage;
	private final Button smallPackageButton;
	private final Button mediumPackageButton;
	private final Button bigPackageButton;
	private final Button priorityButton;
	private final Button parcelLockerButton;


	public ParcelMenu() {
		super();
		setTitle("Parcel lockers menu.");
		setSize(9 * 4);

		this.smallPackageButton = new Button() {
			@Override
			public void onClickedInMenu(final Player player, final Menu menu, final ClickType clickType) {
				restartMenu("&aChanged the package size to small.");
				chosenPackage = 1;
			}

			@Override
			public ItemStack getItem() {
				if (isChosen()) {
					return ItemCreator.of(CompMaterial.CHEST, "&aSmall package", "&bClick to choose a small package.", "", "&aCost: &2$13.99")
							.glow(true)
							.make();
				}
				return ItemCreator.of(CompMaterial.CHEST, "&aSmall package", "&bClick to choose a small package.", "", "&aCost: &2$13.99")
						.make();
			}

			boolean isChosen() {
				return chosenPackage == 1;
			}
		};
		this.mediumPackageButton = new Button() {
			@Override
			public void onClickedInMenu(final Player player, final Menu menu, final ClickType clickType) {
				restartMenu("&aChanged the package size to medium.");
				chosenPackage = 2;
			}

			@Override
			public ItemStack getItem() {
				if (isChosen()) {
					return ItemCreator.of(CompMaterial.CHEST, "&aMedium package &b[Most recommended]", "&bClick to choose a medium package.", "", "&aCost: &2$14.99")
							.glow(true)
							.make();
				}
				return ItemCreator.of(CompMaterial.CHEST, "&aMedium package &b[Most recommended]", "&bClick to choose a medium package.", "", "&aCost: &2$14.99")
						.make();
			}

			boolean isChosen() {
				return chosenPackage == 2;
			}
		};
		this.bigPackageButton = new Button() {
			@Override
			public void onClickedInMenu(final Player player, final Menu menu, final ClickType clickType) {
				restartMenu("&aChanged the package size to big.");
				chosenPackage = 3;
			}

			@Override
			public ItemStack getItem() {
				if (isChosen()) {
					return ItemCreator.of(CompMaterial.CHEST, "&aBig package &6[Most valuable]", "&bClick to choose a big package.", "", "&aCost: &2$16.49")
							.glow(true)
							.make();
				}
				return ItemCreator.of(CompMaterial.CHEST, "&aBig package &6[Most valuable]", "&bClick to choose a big package.", "", "&aCost: &2$16.49")
						.make();
			}

			boolean isChosen() {
				return chosenPackage == 3;
			}
		};
		this.priorityButton = new Button() {
			@Override
			public void onClickedInMenu(final Player player, final Menu menu, final ClickType clickType) {
				restartMenu("&aChanged package priority.");
				priority = !priority;
			}

			@Override
			public ItemStack getItem() {
				return priority ? ItemCreator.of(CompMaterial.NETHER_STAR, "&2Click to add a priority status to package.", "&aCost: $2.99").glow(true).make() : ItemCreator.of(CompMaterial.NETHER_STAR, "&cClick to remove the priority status from the package.").make();
			}

			private boolean priority = false;
		};
		parcelLockerButton = new ButtonMenu(new ParcelSelectionMenu(), CompMaterial.ENDER_CHEST, "&aClick to choose destination parcel locker.");


	}

	@Override
	public ItemStack getItemAt(final int slot) {
		if (slot == 0) return smallPackageButton.getItem();
		if (slot == 1) return mediumPackageButton.getItem();
		if (slot == 2) return bigPackageButton.getItem();
		if (slot == 3) return priorityButton.getItem();
		if (slot == 4) return parcelLockerButton.getItem();
		return ItemCreator.of(CompMaterial.GRAY_STAINED_GLASS_PANE).make();
	}

	@Override
	protected String[] getInfo() {
		return new String[]{"&bMain parcel menu to easily and efficiently send parcels."};
	}

	private final class ParcelSelectionMenu extends Menu {

		protected ParcelSelectionMenu() {
			setSize(9 * 3);
			setTitle("&aParcel selection menu.");
		}

		@Override
		public ItemStack getItemAt(final int slot) {
			return super.getItemAt(slot);
		}

		@Override
		protected String[] getInfo() {
			return super.getInfo();
		}
	}

}
