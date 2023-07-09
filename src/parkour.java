import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.time.Duration;
import java.util.concurrent.TimeUnit;


public class parkour extends JavaPlugin implements Listener {
    private Location Inizio;
    private Location Fine;
    private ArrayList<Location> checkpoints;
    private HashMap<Player, Long> tempo;
    private ArrayList<RegistrazioneTempi> Registrodeitempi;

    private HashMap<Player, Boolean> Parkourincorso;
    private HashMap<Player, Location> Checkpointgiocatore;

    @Override
    public void onEnable() {
        Inizio = new Location(Bukkit.getWorld("world"), 0, 64, 0);
        Fine = new Location(Bukkit.getWorld("world"), 100, 64, 100);
        checkpoints = new ArrayList<>();
        tempo = new HashMap<>();
        Registrodeitempi = new ArrayList<>();
        Parkourincorso = new HashMap<>();
        Checkpointgiocatore = new HashMap<>();
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("inizioparkour")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (player.hasPermission("inizioparkour")) {
                    Inizio = player.getLocation();
                    player.sendMessage("Punto di inizio impostato!");
                    return true;
                } else {
                    player.sendMessage("Non hai i permessi per impostare il punto di inizio del parkour!");
                }
            }
        } else if (command.getName().equalsIgnoreCase("fineparkour")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (player.hasPermission("fineparkour")) {
                    Fine = player.getLocation();
                    player.sendMessage("Punto di arrivo impostato!");
                    return true;
                } else {
                    player.sendMessage("Non hai i permessi per impostare il punto di arrivo del parkour!");
                }
            }

        } else if (command.getName().equalsIgnoreCase("checkpointparkour")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (player.hasPermission("checkpointparkour")) {
                    Location checkpointLocation = player.getLocation();
                    checkpoints.add(checkpointLocation);
                    player.sendMessage("Checkpoint impostato!");
                    return true;
                } else {
                    player.sendMessage("Non hai i permessi per impostare un checkpoint!");
                }
            }
        } else if (command.getName().equalsIgnoreCase("annulla")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (Parkourincorso.containsKey(player) && Parkourincorso.get(player)) {
                    Parkourincorso.remove(player);
                    tempo.remove(player);
                    Checkpointgiocatore.remove(player);
                    player.sendMessage("Percorso annullato. Tempo e checkpoint rimossi.");
                    player.teleport(player.getWorld().getSpawnLocation());
                    return true;
                } else {
                    player.sendMessage("Non stai facendo un parkour al momento.");
                }
            }

        } else if (command.getName().equalsIgnoreCase("pinizio")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (Inizio != null) {
                    player.teleport(Inizio);
                    player.sendMessage("Sei stato teletrasportato al parkour.");
                    return true;
                } else {
                    player.sendMessage("l' inizio del parkour non è stato impostato.");
                }
            }
        }
        if (command.getName().equalsIgnoreCase("tempimigliori")) {
            Tempimigliori(sender);
            return true;
        } else if (command.getName().equalsIgnoreCase("checkpoint")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (Checkpointgiocatore.containsKey(player)) {
                    Location checkpointLocation = Checkpointgiocatore.get(player);
                    player.teleport(checkpointLocation);
                    player.sendMessage("Sei stato teletrasportato al tuo ultimo checkpoint raggiunto.");
                    return true;
                } else {
                    player.sendMessage("Non hai raggiunto alcun checkpoint.");
                }
            }
        }

        return false;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location playerLocation = player.getLocation();

        if (playerLocation.getBlock().equals(Inizio.getBlock())) {
            if (!Parkourincorso.containsKey(player)) {
                Parkourincorso.put(player, true);
                player.sendMessage("Iniziato il parkour!");
                tempo.put(player, System.currentTimeMillis());
                player.sendMessage("Timer avviato! Inizia il parkour.");
            }
        } else if (playerLocation.getBlock().equals(Fine.getBlock())) {
            if (tempo.containsKey(player)) {
                long tempoTrascorso = System.currentTimeMillis() - tempo.get(player);
                tempo.remove(player);
                Parkourincorso.remove(player);
                long ore = TimeUnit.MILLISECONDS.toHours(tempoTrascorso);
                long minuti = TimeUnit.MILLISECONDS.toMinutes(tempoTrascorso) % 60;
                long secondi = TimeUnit.MILLISECONDS.toSeconds(tempoTrascorso) % 60;
                String tempoFormattato= ore + " ore, " + minuti + " minuti, " + secondi + " secondi";
                Registrodeitempi.add(new RegistrazioneTempi(player.getName(), tempoFormattato));
                player.sendMessage("Hai completato il parkour in "+ tempoFormattato);
                //salva su un file di testo tutti i tempi registrati
                try (BufferedWriter writer = new BufferedWriter(new FileWriter("tempiparkour.txt"))) {
                    Collections.sort(Registrodeitempi);
                    for (RegistrazioneTempi registrazione : Registrodeitempi) {
                        String riga = registrazione.NomeGiocatore() + "," + registrazione.TempoGara();
                        writer.write(riga);
                        writer.newLine();
                    }
                } catch (IOException e) {
                    player.sendMessage("Si è verificato un errore durante il salvataggio dei tempi.");
                }
            }
        } else if (checkpoints.contains(playerLocation.getBlock())) {
            Checkpointgiocatore.put(player, playerLocation);
            player.sendMessage("Checkpoint raggiunto! Digitare /checkpoint per tornare qui se si cade.");
        }
    }

    private void Tempimigliori(CommandSender sender) {
        if (Registrodeitempi.isEmpty()) {
            sender.sendMessage("Non ci sono tempi registrati al momento.");
            return;
        }
        Collections.sort(Registrodeitempi);
        RegistrazioneTempi t1 = Registrodeitempi.get(0);
        sender.sendMessage("Il migliore tempo del parkour è stato fatto da: " + t1.NomeGiocatore() + ", in " + t1.TempoGara() + " millisecondi");

    }
    public static class RegistrazioneTempi implements Comparable<RegistrazioneTempi> {
        private String nomegiocatore;
        private String tempoFormattato;

        public RegistrazioneTempi(String nomegiocatore, String tempoFormattato) {
            this.nomegiocatore = nomegiocatore;
            this.tempoFormattato = tempoFormattato;
        }

        public String NomeGiocatore() {
            return nomegiocatore;
        }

        public String TempoGara() {
            return tempoFormattato;
        }

        @Override
        public int compareTo(RegistrazioneTempi other) {
            return CharSequence.compare(tempoFormattato, other.TempoGara());
        }
    }
}

