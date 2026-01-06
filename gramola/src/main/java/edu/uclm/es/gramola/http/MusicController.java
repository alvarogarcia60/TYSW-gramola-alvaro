package edu.uclm.es.gramola.http;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.uclm.es.gramola.model.Playlist;
import edu.uclm.es.gramola.services.MusicService;

@RestController
@RequestMapping("music")
@CrossOrigin(origins = { "http://localhost:4200", "http://127.0.0.1:4200" }) // Cambia el "*" por esto
public class MusicController {

    @Autowired private MusicService musicService;

    @GetMapping("/search")
    public List<Map<String, Object>> search(@RequestParam String texto, @RequestParam String email) {
        return this.musicService.search(texto, email);
    }

    @GetMapping("/getPlaylist")
    public List<Playlist> getMyPlaylist(@RequestParam String email) {
        return this.musicService.getMyPlaylist(email);
    }

    @PostMapping("/add")
    public void add(@RequestBody Map<String, Object> songData, @RequestParam String email) {
        this.musicService.addSong(songData, email);
    }

    @DeleteMapping("/delete-song/{id}")
    public void delete(@PathVariable Long id) {
        this.musicService.deleteSong(id);
    }

    @GetMapping("/devices")
    public List<Map<String, Object>> getDevices(@RequestParam String email) {
        return this.musicService.getDevices(email);
    }

    @GetMapping("/playback-state")
    public Map<String, Object> getPlaybackState(@RequestParam String email) {
        return this.musicService.getPlaybackState(email);
    }
}