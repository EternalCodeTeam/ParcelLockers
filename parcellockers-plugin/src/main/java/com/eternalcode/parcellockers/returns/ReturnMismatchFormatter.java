package com.eternalcode.parcellockers.returns;

import com.eternalcode.multification.notice.Notice;
import com.eternalcode.multification.notice.resolver.chat.ChatContent;
import com.eternalcode.parcellockers.configuration.implementation.MessageConfig;
import com.eternalcode.parcellockers.util.MaterialUtil;
import java.util.Objects;
import java.util.stream.Collectors;

public class ReturnMismatchFormatter {

    private final MessageConfig.ParcelMessages messages;

    public ReturnMismatchFormatter(MessageConfig.ParcelMessages messages) {
        this.messages = Objects.requireNonNull(messages, "Parcel messages cannot be null");
    }

    public String format(ParcelReturnValidationResult result) {
        requireMismatch(result);
        return result.mismatches().stream()
            .map(this::format)
            .collect(Collectors.joining(this.messages.returnMismatchSeparator));
    }

    public Notice notice(ParcelReturnValidationResult result) {
        requireMismatch(result);
        Notice.Builder builder = Notice.builder();
        boolean containsPlaceholder = false;
        for (var part : this.messages.returnItemsMismatch.parts()) {
            builder.withPart(part);
            if (part.content() instanceof ChatContent chat
                && chat.messages().stream().anyMatch(message -> message.contains("{MISMATCHES}"))) {
                containsPlaceholder = true;
            }
        }
        if (!containsPlaceholder) {
            builder.chat("{MISMATCHES}");
        }
        return builder.build();
    }

    private static void requireMismatch(ParcelReturnValidationResult result) {
        Objects.requireNonNull(result, "Validation result cannot be null");
        if (result.matches()) {
            throw new IllegalArgumentException("Matching return has no mismatch reasons");
        }
    }

    private String format(ReturnItemMismatch mismatch) {
        String template = switch (mismatch.type()) {
            case UNEXPECTED_ITEM -> this.messages.returnMismatchUnexpectedItem;
            case INSUFFICIENT_AMOUNT -> this.messages.returnMismatchInsufficientAmount;
            case EXCESS_AMOUNT -> this.messages.returnMismatchExcessAmount;
            case DURABILITY -> this.messages.returnMismatchDurability;
            case ITEM_NAME -> this.messages.returnMismatchItemName;
            case ENCHANTMENTS -> this.messages.returnMismatchEnchantments;
            case LORE -> this.messages.returnMismatchLore;
            case NBT -> this.messages.returnMismatchNbt;
        };

        String formatted = template
            .replace("{ITEM}", MaterialUtil.format(mismatch.item()))
            .replace("{EXPECTED_AMOUNT}", Integer.toString(mismatch.expectedAmount()))
            .replace("{DEPOSITED_AMOUNT}", Integer.toString(mismatch.depositedAmount()));
        if (mismatch.expectedDamage() != null) {
            formatted = formatted.replace("{EXPECTED_DAMAGE}", mismatch.expectedDamage().toString());
        }
        if (mismatch.depositedDamage() != null) {
            formatted = formatted.replace("{DEPOSITED_DAMAGE}", mismatch.depositedDamage().toString());
        }
        return formatted;
    }
}
