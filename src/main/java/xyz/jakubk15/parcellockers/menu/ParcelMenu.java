package xyz.jakubk15.parcellockers.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.MenuPagged;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.button.ButtonMenu;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;
import xyz.jakubk15.parcellockers.ParcelLockersPlugin;
import xyz.jakubk15.parcellockers.model.ParcelLocker;
import xyz.jakubk15.parcellockers.model.ParcelSize;

public class ParcelMenu extends Menu {

	/*
	 * An integer representing chosen package.
	 * 1 = small
	 * 2 = medium
	 * 3 = large
	 */
	private ParcelSize chosenPackage;

	/*
	 * Buttons representing size of each parcel.
	 */

	private final Button smallPackageButton;
	private final Button mediumPackageButton;
	private final Button bigPackageButton;
	private final Button priorityButton;
	private final Button parcelLockerButton;


	public ParcelMenu() {
		super(null);
		this.setTitle("Parcel lockers menu.");
		this.setSize(9 * 4);

		this.smallPackageButton = new Button() {
			@Override
			public void onClickedInMenu(Player player, Menu menu, ClickType clickType) {
				ParcelMenu.this.restartMenu("&aChanged the package size to small.");
				ParcelMenu.this.chosenPackage = ParcelSize.SMALL;
			}

			@Override
			public ItemStack getItem() {
				if (this.isChosen()) {
					return ItemCreator.of(CompMaterial.CHEST, "&aSmall package", "&bClick to choose a small package.", "", "&aCost: &2$13.99")
						.glow(true)
						.make();
				}
				return ItemCreator.of(CompMaterial.CHEST, "&aSmall package", "&bClick to choose a small package.", "", "&aCost: &2$13.99")
					.make();
			}

			boolean isChosen() {
				return ParcelMenu.this.chosenPackage == ParcelSize.SMALL;
			}
		};
		this.mediumPackageButton = new Button() {
			@Override
			public void onClickedInMenu(Player player, Menu menu, ClickType clickType) {
				ParcelMenu.this.restartMenu("&aChanged the package size to medium.");
				ParcelMenu.this.chosenPackage = ParcelSize.MEDIUM;
			}

			@Override
			public ItemStack getItem() {
				if (this.isChosen()) {
					return ItemCreator.of(CompMaterial.CHEST, "&aMedium package &b[Most recommended]", "&bClick to choose a medium package.", "", "&aCost: &2$14.99")
						.glow(true)
						.make();
				}
				return ItemCreator.of(CompMaterial.CHEST, "&aMedium package &b[Most recommended]", "&bClick to choose a medium package.", "", "&aCost: &2$14.99")
					.make();
			}

			boolean isChosen() {
				return ParcelMenu.this.chosenPackage == ParcelSize.MEDIUM;
			}
		};
		this.bigPackageButton = new Button() {
			@Override
			public void onClickedInMenu(Player player, Menu menu, ClickType clickType) {
				ParcelMenu.this.restartMenu("&aChanged the package size to big.");
				ParcelMenu.this.chosenPackage = ParcelSize.LARGE;
			}

			@Override
			public ItemStack getItem() {
				if (this.isChosen()) {
					return ItemCreator.of(CompMaterial.CHEST, "&aBig package &6[Most valuable]", "&bClick to choose a big package.", "", "&aCost: &2$16.49")
						.glow(true)
						.make();
				}
				return ItemCreator.of(CompMaterial.CHEST, "&aBig package &6[Most valuable]", "&bClick to choose a big package.", "", "&aCost: &2$16.49")
					.make();
			}

			boolean isChosen() {
				return ParcelMenu.this.chosenPackage == ParcelSize.LARGE;
			}
		};
		this.priorityButton = new Button() {
			private boolean priority = false;

			@Override
			public void onClickedInMenu(Player player, Menu menu, ClickType clickType) {
				ParcelMenu.this.restartMenu("&aChanged package priority.");
				this.priority = !this.priority;
			}

			@Override
			public ItemStack getItem() {
				return this.priority ? ItemCreator.of(CompMaterial.NETHER_STAR, "&2Click to add a priority status to package.", "&aCost: $2.99").glow(true).make() : ItemCreator.of(CompMaterial.NETHER_STAR, "&cClick to remove the priority status from the package.").make();
			}

		};
		this.parcelLockerButton = new ButtonMenu(new ParcelLockerSelectionMenu(), CompMaterial.ENDER_CHEST, "&aClick to choose destination parcel locker.");

	}

	@Override
	public ItemStack getItemAt(int slot) {
		if (slot == 10) return this.smallPackageButton.getItem();
		if (slot == 12) return this.mediumPackageButton.getItem();
		if (slot == 14) return this.bigPackageButton.getItem();
		if (slot == 16) return this.priorityButton.getItem();
		if (slot == 4) return this.parcelLockerButton.getItem();
		return ItemCreator.of(CompMaterial.GRAY_STAINED_GLASS_PANE).make();
	}

	@Override
	protected String[] getInfo() {
		return new String[] {"&bMain parcel menu to easily and efficiently send parcels."};
	}

	private static final class ParcelLockerSelectionMenu extends MenuPagged<ParcelLocker> {

		@Override
		protected ItemStack convertToItemStack(ParcelLocker locker) {
			return ItemCreator.of(CompMaterial.CHEST, "&aParcel locker #" + locker.getUniqueId(), "&bClick to choose this parcel locker.",
				"", "&bName: " + locker.getName(), "",
				"&bID: #" + locker.getUniqueId(), "",
				"&bParcels stored: " + locker.getParcelMap().size(), "",
				"&bLocation: " + Common.shortLocation(locker.getLocation()).toUpperCase()).make();
		}

		@Override
		protected void onPageClick(Player player, ParcelLocker item, ClickType click) {

		}

		@Override
		public ItemStack getItemAt(int slot) {
			for (ParcelLocker parcelLocker : ParcelLockersPlugin.getInstance().getParcelDatabase().keySet()) {
				return this.convertToItemStack(parcelLocker);
			}

			return ItemCreator.of(CompMaterial.GRAY_STAINED_GLASS_PANE, "", "&c&m-|--|-", "&cEmpty option", "&cPick another slot", "&c&m-|--|-").make();
		}

		@Override
		protected String[] getInfo() {
			return new String[] {"&aParcel locker selection menu.", "&aClick on a parcel locker to choose it."};
		}
	}

}
