package edu.uclm.es.gramola;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import edu.uclm.es.gramola.dao.PlaylistDao;
import edu.uclm.es.gramola.dao.UserDao;
import edu.uclm.es.gramola.model.Playlist;
import edu.uclm.es.gramola.model.User;
import edu.uclm.es.gramola.services.MusicService;

@SpringBootTest
@Transactional
@SuppressWarnings("unused") // El método @BeforeEach es usado por JUnit
public class MusicServicePaymentTests {

    @Autowired private MusicService musicService;
    @Autowired private PlaylistDao playlistDao;
    @Autowired private UserDao userDao;

    private final String barEmail = "bar@example.com";

    @BeforeEach
    void setupUser() {
        if (userDao.findById(barEmail).isEmpty()) {
            User u = new User();
            u.setEmail(barEmail);
            u.setBar("Bar de Prueba");
            u.setClientId("client_id_test");
            u.setClientSecret("client_secret_test");
            // Sin token de Spotify para tests; la cola de Spotify se omitirá
            u.setSpotiSimpleToken(null);
            u.setPwd("pwd_test");
            userDao.save(u);
        }
    }

    @Test
    void testAddSongPaidFailsWithInvalidPaymentIntentId() {
        Map<String, Object> songData = new HashMap<>();
        songData.put("name", "Test Song");
        Map<String, Object> album = new HashMap<>();
        album.put("images", List.of());
        songData.put("album", album);
        songData.put("artists", List.of());
        songData.put("uri", "spotify:track:testid");

        boolean ok = musicService.addSongPaid(songData, barEmail, "pi_invalid_test");
        assertFalse(ok, "El pago inválido debe impedir insertar la canción");
    }

    @Test
    void testInsertAtPositionTwoOrder() {
        // Construir una cola inicial: [A(pos1), B(pos2), C(pos3)]
        Playlist a = new Playlist(); a.setBarEmail(barEmail); a.setTitle("A"); a.setArtist("Artist A"); a.setQueuePosition(1); playlistDao.save(a);
        Playlist b = new Playlist(); b.setBarEmail(barEmail); b.setTitle("B"); b.setArtist("Artist B"); b.setQueuePosition(2); playlistDao.save(b);
        Playlist c = new Playlist(); c.setBarEmail(barEmail); c.setTitle("C"); c.setArtist("Artist C"); c.setQueuePosition(3); playlistDao.save(c);

        Map<String, Object> songData = new HashMap<>();
        songData.put("name", "Cliente1");
        Map<String, Object> album = new HashMap<>();
        album.put("images", List.of());
        songData.put("album", album);
        Map<String, Object> artist1 = new HashMap<>();
        artist1.put("name", "Artist Cliente1");
        songData.put("artists", List.of(artist1));
        songData.put("uri", "spotify:track:cliente1");

        musicService.addSong(songData, barEmail);

        List<Playlist> queue = playlistDao.findByBarEmailOrderByQueuePositionAsc(barEmail);
        assertEquals(4, queue.size());
        assertEquals("A", queue.get(0).getTitle()); // sigue sonando A
        assertEquals("Cliente1", queue.get(1).getTitle()); // colada en posición 2
        assertEquals("B", queue.get(2).getTitle()); // desplazada a 3
        assertEquals("C", queue.get(3).getTitle()); // desplazada a 4
    }
}
