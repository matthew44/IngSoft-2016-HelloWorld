package main.java.models;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;

import javazoom.jlgui.basicplayer.BasicPlayer;
import javazoom.jlgui.basicplayer.BasicPlayerException;
import main.java.views.BPMObserver;
import main.java.views.BeatObserver;
import main.java.views.MP3View;
import main.java.views.TrackObserver;
import main.java.states.*;

public class MP3Model implements MP3ModelInterface {
	
	private static MP3Model uniqueMP3 = null;
	
	private BasicPlayer player;
	private ArrayList<String> playlist;
	private ArrayList<BPMObserver> bpmObservers;
	private ArrayList<BeatObserver> beatObservers;
	private ArrayList<TrackObserver> trackObservers;
	//States
	private MP3State currentState;
	private MP3State playing;
	private MP3State stopped;
	private MP3State empty;
	private MP3State paused;
	//-----------------------------
	private int index;
	private double volumen;

	/*
	public static class ProgressBarListener implements Runnable {
		private static class ProgressBarListenerHolder {
			public static ProgressBarListener uniqueProgressBarListener = new ProgressBarListener();
		}
		
		private static MP3View view = null;

		private ProgressBarListener(){}
		private void setView(MP3View viewInstance){
			view = viewInstance;
		}
		
		public static ProgressBarListener getInstance(MP3View viewInstance){
			if(ProgressBarListenerHolder.uniqueProgressBarListener==null){
				ProgressBarListenerHolder.uniqueProgressBarListener.setView(viewInstance);
			} else if (view==null){
				view = viewInstance;
			}
			return ProgressBarListenerHolder.uniqueProgressBarListener;
		}
		
		public void run() {
			while (view.updateProgressBar()) {
				try {
					Thread.sleep(500);
				} catch (Exception e) {}
			}
		}
	}
	*/
	
	private MP3Model(){
		this.player = new BasicPlayer();
		this.bpmObservers = new ArrayList<BPMObserver>();
		this.beatObservers = new ArrayList<BeatObserver>();
		this.index = 0;	
		this.volumen = 1;			//maximo volumen
		this.empty = new EmptyState(this);
		this.stopped = new StoppedState(this);
		this.paused = new PausedState(this);
		this.playing = new PlayingState(this);
		this.currentState = empty;
		this.playlist = new ArrayList<String>();
		this.trackObservers = new ArrayList<TrackObserver>();
	}
	
	public static MP3Model getInstance(){
		if (uniqueMP3 == null){
			uniqueMP3 = new MP3Model();
		}
		return uniqueMP3;
	}

	@Override
	public void play() {
		currentState.play();
		notifyBPMObservers();
		notifyTrackObservers();
		try {
			player.setGain(volumen);
		} catch (BasicPlayerException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void pause() {
		currentState.paused();
	}

	@Override
	public void addPlayList(String Path) {
		File file = new File(Path);				// Uso la ruta para crear un nuevo File
		if (file.isFile()) { 					// Si la ruta es una sola cancion
			playlist.add(Path);
		} else { 								// Si la ruta es una carpeta con canciones
			File list[] = file.listFiles();
			if (list != null) {
				for (int i = 0; i < list.length; i++) {
					if(list[i].getAbsolutePath().endsWith(".mp3")){
						playlist.add(list[i].getAbsolutePath());
					}
				}
			}
		}
		currentState.addPlaylist();
		notifyTrackObservers();
	}

	@Override
	public void previousSong() {
		currentState.previousSong();
		notifyTrackObservers();
		notifyBPMObservers();
	}

	@Override
	public void nextSong() {
		currentState.nextSong();
		notifyTrackObservers();
		notifyBPMObservers();
	}

	@Override
	public void stop() {
		currentState.stop();
	}

	@Override
	public void setVolumen(double volumen) {
		if(volumen<0 || volumen>1){
			return;
		}
		else{
			this.volumen = volumen;
			try {
				player.setGain(volumen);
			} catch (BasicPlayerException e) {
				e.printStackTrace();
			}
		}
	}
	
	public double getVolumen(){
		return volumen;
	}
	
	public void registerObserver(TrackObserver o){
		trackObservers.add(o);
	}
	
	public void removeObserver(TrackObserver o){
		int i = trackObservers.indexOf(o);
		if (i >= 0){
			trackObservers.remove(i);
		}
	}

	public void notifyTrackObservers(){
		for (int i = 0; i < trackObservers.size(); i++){
			TrackObserver observer = trackObservers.get(i);
			observer.updateTrackInfo();
			observer.updatePlaylistInfo();
		}
	}
	
	public void registerObserver(BPMObserver o) {
		bpmObservers.add(o);
	}
	
	public void removeObserver(BPMObserver o) {
		int i = bpmObservers.indexOf(o);
		if (i >= 0) {
			bpmObservers.remove(i);
		}
	}

	public void notifyBPMObservers() {
		for (int i = 0; i < bpmObservers.size(); i++) {
			BPMObserver observer = (BPMObserver) bpmObservers.get(i);
			observer.updateBPM();
		}
	}

	public void registerObserver(BeatObserver o) {
		beatObservers.add(o);	
	}
	
	public void removeObserver(BeatObserver o) {
		int i = beatObservers.indexOf(o);
		if (i >= 0) {
			beatObservers.remove(i);
		}
	}
	
	public boolean setIndex (int index){
		if ( index > (playlist.size()-1) || index < 0)
			return false;
		this.index = index;
		return true;
	}

	@Override
	public int getIndex() {
		return index;
	}
	
	public String getCurrentTrackName(){		
		String path = playlist.get(index);
		Mp3File mp3file = null;
		try {
			mp3file = new Mp3File(path);
		} catch (UnsupportedTagException | InvalidDataException | IOException e) {
			e.printStackTrace();
		}
		ID3v2 songTag = mp3file.getId3v2Tag();
		return songTag.getTitle();
	}

	public String getCurrentSongDuration(){
		String path = playlist.get(index);
		Mp3File song = null;
		try {
			song = new Mp3File(path);
		} catch (UnsupportedTagException | InvalidDataException | IOException e) {
			e.printStackTrace();
		}
		long duration = song.getLengthInSeconds();
		int minutes = (int)duration/60;
		int seconds = (int)duration%60;
		String songDuration = String.format("%02d:%02d", minutes, seconds);
		return songDuration;
	}
	
	public long getCurrentSongDurationMil(){
		String path = playlist.get(index);
		Mp3File song = null;
		try {
			song = new Mp3File(path);
		} catch (UnsupportedTagException | InvalidDataException | IOException e) {
			e.printStackTrace();
		}
		long duration = song.getLengthInMilliseconds();
		return duration;
	}

	@Override
	public boolean IsPlaying() {
		if (currentState instanceof PlayingState ){
			return true;
		}
		else{
			return false;
		}
	}

	@Override
	public String[] getCurrentPlaylist() {
		String[] playlistArray = new String[playlist.size()];
		Mp3File song = null;
		ID3v2 songTag = null;
		for(int i=0;i<playlist.size();i++){
			String path = playlist.get(i);
			try {
				song = new Mp3File(path);
			} catch (UnsupportedTagException | InvalidDataException | IOException e) {
				e.printStackTrace();
			}
			songTag = song.getId3v2Tag();
			playlistArray[i] = songTag.getTitle();
		}
		return playlistArray;
	}
	
	@Override
	public ID3v2 getSongInfo() {
		Mp3File song = null;
		try {
			song = new Mp3File(playlist.get(index));
		} catch (UnsupportedTagException | InvalidDataException | IOException e) {
			e.printStackTrace();
		}
		if (song.hasId3v2Tag()) {
			ID3v2 id3v2Tag = song.getId3v2Tag();
			return id3v2Tag;
		}
		return null;
	}
	
	@Override
	public ID3v2 getAlbumArt(){
		Mp3File song = null;
		try {
			song = new Mp3File(playlist.get(index));
		} catch (UnsupportedTagException | InvalidDataException | IOException e) {
			e.printStackTrace();
		}
		if (song.hasId3v2Tag()) {
			ID3v2 id3v2Tag = song.getId3v2Tag();
			return id3v2Tag;
        }
		return null;
	}
	
	@Override
	public void clearPlaylist() {
		playlist.clear();
//		notifyTrackObservers();
	}
	
	public void setState(MP3State newState){
		this.currentState = newState;
	}
	
	public MP3State getPlayingState(){return playing;}
	public MP3State getPausedState() {return paused;}
	public MP3State getEmptyState()  {return empty;}
	public MP3State getStoppedState(){return stopped;}
	
	public BasicPlayer getPlayer(){
		return player;
	}
	
	public int getPlaylistSize(){
		return playlist.size();
	}
	
	//Metodo usado por el estado, para no perder encapsulamiento.
	//En lugar de proveer acceso al arraylist playlist simplemente 
	//muestro el indice de la cancion a reproducir con un getter, y proveo
	//el metodo playNow para que comience a sonar segun el indice que le paso
	public void playNow(int index){
		File f = new File(playlist.get(index));
		try {
			player.open(f);
		} catch (BasicPlayerException e) {
			e.printStackTrace();
		}
		try {
			player.play();
		} catch (BasicPlayerException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void removePlayList(int index) {
		if(this.IsPlaying() && this.getIndex() == index){
			if(this.getPlaylistSize()>1){
				this.nextSong();
			} else {
				this.stop();
			}
		}
		if(playlist != null && !playlist.isEmpty()){
			playlist.remove(index);
		}
	}

	@Override
	public void setTime(long time) {
		if(!(this.currentState instanceof EmptyState) || !(this.currentState instanceof StoppedState)){
			try {
				this.player.seek(time/34);
			} catch (BasicPlayerException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void addPlayerListener(MP3View mp3View, long t) {
		this.player.addBasicPlayerListener(MP3View.PlayerListener.getInstance(mp3View, t));
	}
	
}