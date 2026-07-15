package com.eternalcode.parcellockers.returns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.eternalcode.multification.notice.Notice;
import com.eternalcode.multification.notice.resolver.chat.ChatContent;
import com.eternalcode.parcellockers.configuration.implementation.MessageConfig;
import java.util.List;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class ReturnMismatchFormatterTest {

    @Test
    void formatsEveryConfiguredReasonWithItemSpecificValues() {
        MessageConfig.ParcelMessages messages = new MessageConfig.ParcelMessages();
        messages.returnMismatchSeparator = " | ";
        messages.returnMismatchUnexpectedItem = "unexpected:{ITEM}:{DEPOSITED_AMOUNT}";
        messages.returnMismatchInsufficientAmount = "insufficient:{ITEM}:{EXPECTED_AMOUNT}/{DEPOSITED_AMOUNT}";
        messages.returnMismatchExcessAmount = "excess:{ITEM}:{EXPECTED_AMOUNT}/{DEPOSITED_AMOUNT}";
        messages.returnMismatchDurability = "durability:{ITEM}:{EXPECTED_DAMAGE}/{DEPOSITED_DAMAGE}";
        messages.returnMismatchItemName = "name:{ITEM}";
        messages.returnMismatchEnchantments = "enchantments:{ITEM}";
        messages.returnMismatchLore = "lore:{ITEM}";
        messages.returnMismatchNbt = "nbt:{ITEM}";
        ReturnMismatchFormatter formatter = new ReturnMismatchFormatter(messages);

        String formatted = formatter.format(new ParcelReturnValidationResult(List.of(
            ReturnItemMismatch.unexpected(Material.DIRT, 2),
            ReturnItemMismatch.insufficient(Material.DIAMOND, 5, 4),
            ReturnItemMismatch.excess(Material.OAK_LOG, 64, 65),
            new ReturnItemMismatch(ReturnMismatchType.DURABILITY, Material.DIAMOND_SWORD, 0, 0, 4, 27),
            new ReturnItemMismatch(ReturnMismatchType.ITEM_NAME, Material.WRITTEN_BOOK, 0, 0, null, null),
            new ReturnItemMismatch(ReturnMismatchType.ENCHANTMENTS, Material.BOW, 0, 0, null, null),
            new ReturnItemMismatch(ReturnMismatchType.LORE, Material.PAPER, 0, 0, null, null),
            new ReturnItemMismatch(ReturnMismatchType.NBT, Material.SHULKER_BOX, 0, 0, null, null)
        )));

        assertEquals(
            "unexpected:Dirt:2 | insufficient:Diamond:5/4 | excess:Oak log:64/65"
                + " | durability:Diamond sword:4/27 | name:Written book | enchantments:Bow"
                + " | lore:Paper | nbt:Shulker box",
            formatted
        );
    }

    @Test
    void appendsMismatchPlaceholderToLegacyConfiguredNotice() {
        MessageConfig.ParcelMessages messages = new MessageConfig.ParcelMessages();
        messages.returnItemsMismatch = Notice.chat("legacy mismatch message");
        ReturnMismatchFormatter formatter = new ReturnMismatchFormatter(messages);
        ParcelReturnValidationResult mismatch = new ParcelReturnValidationResult(List.of(
            ReturnItemMismatch.unexpected(Material.DIRT, 2)
        ));

        Notice notice = formatter.notice(mismatch);
        ChatContent chat = (ChatContent) notice.parts().getFirst().content();

        assertEquals(List.of("legacy mismatch message", "{MISMATCHES}"), chat.messages());
    }

    @Test
    void rejectsMatchingResultBecauseThereAreNoReasonsToFormat() {
        ReturnMismatchFormatter formatter = new ReturnMismatchFormatter(new MessageConfig.ParcelMessages());

        assertThrows(
            IllegalArgumentException.class,
            () -> formatter.format(new ParcelReturnValidationResult(List.of()))
        );
    }
}
