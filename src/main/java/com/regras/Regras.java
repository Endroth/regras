package com.regras;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class Regras extends JavaPlugin implements Listener {

    private NamespacedKey editorKey;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        editorKey = new NamespacedKey(this, "editor");
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Só jogadores podem usar esse comando.");
            return true;
        }
        if (p.isOp()) {
            darLivroEditavel(p);
        } else {
            abrirLivro(p);
        }
        return true;
    }

    // Jogador normal: livro assinado (só leitura)
    private void abrirLivro(Player p) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.title(Component.text("Regras"));
        meta.author(Component.text("Servidor"));
        for (String page : getConfig().getStringList("pages")) {
            meta.addPages(LegacyComponentSerializer.legacyAmpersand().deserialize(page));
        }
        book.setItemMeta(meta);
        p.openBook(book);
    }

    // OP: livro e pena editável
    private void darLivroEditavel(Player p) {
        ItemStack book = new ItemStack(Material.WRITABLE_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        for (String page : getConfig().getStringList("pages")) {
            meta.addPages(Component.text(page));
        }
        meta.displayName(Component.text("§eEditor de Regras"));
        meta.getPersistentDataContainer().set(editorKey, PersistentDataType.BYTE, (byte) 1);
        book.setItemMeta(meta);

        var sobra = p.getInventory().addItem(book);
        sobra.values().forEach(i -> p.getWorld().dropItem(p.getLocation(), i));

        p.sendMessage("§aVocê recebeu o livro das regras. Segure ele e clique com o botão direito para editar.");
        p.sendMessage("§7Dica: use & para cores (ex: &c vermelho, &l negrito). Clique em 'Concluído' pra salvar.");
    }

    @EventHandler
    public void aoEditar(PlayerEditBookEvent e) {
        Player p = e.getPlayer();
        if (!p.isOp()) return;

        int slot = e.getSlot();
        ItemStack item = slot == -1 ? p.getInventory().getItemInOffHand() : p.getInventory().getItem(slot);
        if (item == null || !item.hasItemMeta()) return;
        if (!item.getItemMeta().getPersistentDataContainer().has(editorKey, PersistentDataType.BYTE)) return;

        List<String> pages = new ArrayList<>();
        for (Component c : e.getNewBookMeta().pages()) {
            pages.add(PlainTextComponentSerializer.plainText().serialize(c));
        }
        getConfig().set("pages", pages);
        saveConfig();

        e.setCancelled(true);
        if (slot == -1) {
            p.getInventory().setItemInOffHand(null);
        } else {
            p.getInventory().setItem(slot, null);
        }
        p.sendMessage("§aRegras salvas! Use /regras pra ver como ficou.");
    }
}
