package com.eternalcode.parcellockers.util.serializer;

import com.eternalcode.parcellockers.exception.ParcelLockersException;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

// https://gist.github.com/graywolf336/8153678?permalink_comment_id=4536153#gistcomment-4536153
public class ItemBase64Serializer {

    /**
     * Gets one {@link ItemStack} from Base64 string.
     *
     * @param data Base64 string to convert to {@link ItemStack}.
     * @return {@link ItemStack} created from the Base64 string.
     * @throws IOException
     */
    public static ItemStack deserialize(String data) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decode(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack item;

            // Read the serialized itemstack
            item = (ItemStack) dataInput.readObject();

            dataInput.close();
            return item;
        } catch (ClassNotFoundException | IOException e) {
            throw new ParcelLockersException("Unable to decode class type.", e);
        }
    }

    /**
     * A method to serialize one {@link ItemStack} to Base64 String.
     *
     * @param item to turn into a Base64 String.
     * @return Base64 string of the item.
     * @throws IllegalStateException
     */
    public static String serialize(ItemStack item) throws IllegalStateException {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

            // Save every element
            dataOutput.writeObject(item);

            // Serialize that array
            dataOutput.close();
            return new String(Base64Coder.encode(outputStream.toByteArray()));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save item stacks.", e);
        }
    }

}
