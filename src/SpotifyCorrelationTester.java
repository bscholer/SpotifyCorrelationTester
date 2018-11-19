import com.neovisionaries.i18n.CountryCode;
import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.SpotifyHttpManager;
import com.wrapper.spotify.enums.ModelObjectType;
import com.wrapper.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import com.wrapper.spotify.model_objects.special.SearchResult;
import com.wrapper.spotify.model_objects.specification.*;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeRefreshRequest;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;
import com.wrapper.spotify.requests.data.albums.GetAlbumsTracksRequest;
import com.wrapper.spotify.requests.data.artists.GetArtistsAlbumsRequest;
import com.wrapper.spotify.requests.data.library.GetCurrentUsersSavedAlbumsRequest;
import com.wrapper.spotify.requests.data.playlists.GetListOfUsersPlaylistsRequest;
import com.wrapper.spotify.requests.data.playlists.GetPlaylistsTracksRequest;
import com.wrapper.spotify.requests.data.search.SearchItemRequest;
import com.wrapper.spotify.requests.data.tracks.GetAudioFeaturesForSeveralTracksRequest;
import com.wrapper.spotify.requests.data.users_profile.GetCurrentUsersProfileRequest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class SpotifyCorrelationTester {

    static String prompt1 = "Would you like to:\n[1] Use an item from your library\n[2] Search for an item\nChoice: ";
    static String typePromptWithoutArtist = "Would you like to use:\n[1] Playlist\n[2] Album\nChoice: ";
    static String typePromptWithArtist = "Would you like to use:\n[1] Playlist\n[2] Album\n[3] Artist\nChoice: ";
    static String userId;

    public static void main(String args[]) {
        Scanner scanner = new Scanner(System.in);
        ArrayList<AudioFeatures> audioFeatures;
        int choice;
        String query;

        System.out.println("Spotify Track Group Feature Analyzer\n");
        SpotifyApi spotifyApi = authenticateSpotify();
        getUserId(spotifyApi);
        System.out.print(prompt1);

        choice = scanner.nextInt();
        switch (choice) {
            case 1:
                System.out.println("\n" + typePromptWithoutArtist);
                choice = scanner.nextInt();
                switch (choice) {
                    //Playlist
                    case 1:
                        PlaylistSimplified[] playlistSimps = getUsersPlaylists(spotifyApi);
                        System.out.println("\nSelect a playlist:");
                        for (int i = 0; i < playlistSimps.length; i++) {
                            System.out.printf("[%d] %s\n", i + 1, getPlaylistString(playlistSimps[i]));
                        }
                        System.out.print("Choice: ");
                        choice = scanner.nextInt();
                        audioFeatures = getTrackAudioFeatures(spotifyApi, playlistSimps[choice - 1]);
                        findLikeness(audioFeatures);
                        break;
                    //Album
                    case 2:
                        Album[] albums = getUsersAlbums(spotifyApi);
                        System.out.println("\nSelect an album:");
                        for (int i = 0; i < albums.length; i++) {
                            System.out.printf("[%d] %s\n", i + 1, getAlbumString(albums[i]));
                        }
                        System.out.print("Choice: ");
                        choice = scanner.nextInt();
                        audioFeatures = getTrackAudioFeatures(spotifyApi, albums[choice - 1]);
                        findLikeness(audioFeatures);
                        break;
                }
                break;
            case 2:
                System.out.print("\n" + typePromptWithArtist);
                choice = scanner.nextInt();
                scanner.nextLine();
                switch (choice) {
                    //Playlist
                    case 1:
                        //Search
                        System.out.print("Playlist query: ");
                        query = scanner.nextLine();
                        PlaylistSimplified[] playlistSimps = searchForPlaylist(spotifyApi, query);
                        for (int i = 0; i < playlistSimps.length; i++) {
                            System.out.printf("[%d] %s\n", i + 1, getPlaylistString(playlistSimps[i]));
                        }
                        System.out.print("Choice: ");
                        choice = scanner.nextInt();
                        audioFeatures = getTrackAudioFeatures(spotifyApi, playlistSimps[choice - 1]);
                        findLikeness(audioFeatures);
                        break;
                    //Album
                    case 2:
                        System.out.print("Album query: ");
                        query = scanner.nextLine();
                        AlbumSimplified[] albums = searchForAlbum(spotifyApi, query);
                        System.out.println("\nSelect an album:");
                        for (int i = 0; i < albums.length; i++) {
                            System.out.printf("[%d] %s\n", i + 1, getAlbumSimplifiedString(albums[i]));
                        }
                        System.out.print("Choice: ");
                        choice = scanner.nextInt();
                        audioFeatures = getTrackAudioFeatures(spotifyApi, albums[choice - 1]);
                        findLikeness(audioFeatures);
                        break;
                    //Artist
                    case 3:
                        System.out.print("Artist query: ");
                        query = scanner.nextLine();
                        Artist[] artists = searchForArtist(spotifyApi, query);
                        System.out.println("\nSelect an artist:");
                        for (int i = 0; i < artists.length; i++) {
                            System.out.printf("[%d] %s\n", i + 1, artists[i].getName());
                        }
                        System.out.print("Choice: ");
                        choice = scanner.nextInt();
                        GetArtistsAlbumsRequest getArtistsAlbumsRequest = spotifyApi
                                .getArtistsAlbums(artists[choice - 1].getId())
                                .limit(10)
                                .build();
                        try {
                            audioFeatures = getTrackAudioFeatures(spotifyApi, getArtistsAlbumsRequest.execute().getItems());
                            findLikeness(audioFeatures);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        break;
                }
                break;
        }
    }

    static PlaylistSimplified[] getUsersPlaylists(SpotifyApi spotifyApi) {
        GetListOfUsersPlaylistsRequest getListOfUsersPlaylistsRequest = spotifyApi
                .getListOfUsersPlaylists(userId)
                .limit(50)
                .build();
        try {
            Paging<PlaylistSimplified> playlistSimplifiedPaging = getListOfUsersPlaylistsRequest.execute();
            return playlistSimplifiedPaging.getItems();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    static Album[] getUsersAlbums(SpotifyApi spotifyApi) {
        GetCurrentUsersSavedAlbumsRequest getCurrentUsersSavedAlbumsRequest = spotifyApi
                .getCurrentUsersSavedAlbums()
                .limit(50)
                .build();
        try {
            Paging<SavedAlbum> savedAlbumPaging = getCurrentUsersSavedAlbumsRequest.execute();
            SavedAlbum[] savedAlbums = savedAlbumPaging.getItems();
            Album[] albums = new Album[savedAlbumPaging.getTotal()];
            for (int i = 0; i < savedAlbums.length; i++) {
                albums[i] = savedAlbums[i].getAlbum();
            }
            return albums;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    static PlaylistSimplified[] searchForPlaylist(SpotifyApi spotifyApi, String query) {
        SearchItemRequest searchItemRequest = spotifyApi
                .searchItem(query, ModelObjectType.PLAYLIST.getType())
                .market(CountryCode.US)
                .limit(10)
                .build();
        try {
            SearchResult searchResult = searchItemRequest.execute();
            return searchResult.getPlaylists().getItems();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    static AlbumSimplified[] searchForAlbum(SpotifyApi spotifyApi, String query) {
        SearchItemRequest searchItemRequest = spotifyApi
                .searchItem(query, ModelObjectType.ALBUM.getType())
                .market(CountryCode.US)
                .limit(10)
                .build();
        try {
            SearchResult searchResult = searchItemRequest.execute();
            return searchResult.getAlbums().getItems();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    static Artist[] searchForArtist(SpotifyApi spotifyApi, String query) {
        SearchItemRequest searchItemRequest = spotifyApi
                .searchItem(query, ModelObjectType.ARTIST.getType())
                .market(CountryCode.US)
                .limit(10)
                .build();
        try {
            SearchResult searchResult = searchItemRequest.execute();
            return searchResult.getArtists().getItems();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    static ArrayList<AudioFeatures> getTrackAudioFeatures(SpotifyApi spotifyApi, PlaylistSimplified playlistSimplified) {
        ArrayList<AudioFeatures> audioFeatures = new ArrayList<>();
        int total = playlistSimplified.getTracks().getTotal();
        for (int i = 0; i < Math.ceil(total / 100.0); i++) {
            GetPlaylistsTracksRequest getPlaylistsTracksRequest = spotifyApi
                    .getPlaylistsTracks(playlistSimplified.getId())
                    .offset(i * 100)
                    .build();
            try {
                Paging<PlaylistTrack> playlistTracks = getPlaylistsTracksRequest.execute();
                PlaylistTrack[] playlistTracksArray = playlistTracks.getItems();
                String[] ids = new String[playlistTracksArray.length];
                for (int j = 0; j < playlistTracksArray.length; j++) {
                    ids[j] = playlistTracksArray[j].getTrack().getId();
                }
                GetAudioFeaturesForSeveralTracksRequest gaffstr = spotifyApi
                        .getAudioFeaturesForSeveralTracks(ids)
                        .build();
                AudioFeatures[] features = gaffstr.execute();
                audioFeatures.addAll(Arrays.asList(features));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return audioFeatures;
    }

    static ArrayList<AudioFeatures> getTrackAudioFeatures(SpotifyApi spotifyApi, Album album) {
        ArrayList<AudioFeatures> audioFeatures = new ArrayList<>();
        int total = album.getTracks().getTotal();
        for (int i = 0; i < Math.ceil(total / 100.0); i++) {
            GetAlbumsTracksRequest getAlbumsTracksRequest = spotifyApi
                    .getAlbumsTracks(album.getId())
                    .offset(i * 100)
                    .build();
            try {
                Paging<TrackSimplified> albumTracks = getAlbumsTracksRequest.execute();

                TrackSimplified[] trackSimplifieds = albumTracks.getItems();
                String[] uris = new String[trackSimplifieds.length];
                for (int j = 0; j < trackSimplifieds.length; j++) {
                    uris[j] = trackSimplifieds[j].getId();
                }
                GetAudioFeaturesForSeveralTracksRequest gaffstr = spotifyApi
                        .getAudioFeaturesForSeveralTracks(uris)
                        .build();
                AudioFeatures[] features = gaffstr.execute();
                audioFeatures.addAll(Arrays.asList(features));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return audioFeatures;
    }

    static ArrayList<AudioFeatures> getTrackAudioFeatures(SpotifyApi spotifyApi, AlbumSimplified album) {
        ArrayList<AudioFeatures> audioFeatures = new ArrayList<>();
        GetAlbumsTracksRequest getAlbumsTracksRequest = spotifyApi
                .getAlbumsTracks(album.getId())
                .build();
        try {
            Paging<TrackSimplified> albumTracks = getAlbumsTracksRequest.execute();

            TrackSimplified[] trackSimplifieds = albumTracks.getItems();
            String[] uris = new String[trackSimplifieds.length];
            for (int j = 0; j < trackSimplifieds.length; j++) {
                uris[j] = trackSimplifieds[j].getId();
            }
            GetAudioFeaturesForSeveralTracksRequest gaffstr = spotifyApi
                    .getAudioFeaturesForSeveralTracks(uris)
                    .build();
            AudioFeatures[] features = gaffstr.execute();
            audioFeatures.addAll(Arrays.asList(features));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return audioFeatures;
    }

    static ArrayList<AudioFeatures> getTrackAudioFeatures(SpotifyApi spotifyApi, AlbumSimplified[] albums) {
        ArrayList<AudioFeatures> audioFeatures = new ArrayList<>();
        String allAlbums = "";
        for (AlbumSimplified album : albums) {
            allAlbums += ", " + album.getName();
            GetAlbumsTracksRequest getAlbumsTracksRequest = spotifyApi
                    .getAlbumsTracks(album.getId())
                    .build();
            try {
                Paging<TrackSimplified> albumTracks = getAlbumsTracksRequest.execute();

                TrackSimplified[] trackSimplifieds = albumTracks.getItems();
                String[] uris = new String[trackSimplifieds.length];
                for (int j = 0; j < trackSimplifieds.length; j++) {
                    uris[j] = trackSimplifieds[j].getId();
                }
                GetAudioFeaturesForSeveralTracksRequest gaffstr = spotifyApi
                        .getAudioFeaturesForSeveralTracks(uris)
                        .build();
                AudioFeatures[] features = gaffstr.execute();
                audioFeatures.addAll(Arrays.asList(features));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        allAlbums = allAlbums.substring(2);
        System.out.println("\nLooking at audio features for: " + allAlbums);
        return audioFeatures;
    }

    static void findLikeness(ArrayList<AudioFeatures> audioFeatures) {
        float[] danceability = new float[audioFeatures.size()];
        float[] energy = new float[audioFeatures.size()];
        int[] key = new int[audioFeatures.size()];
        float[] loudness = new float[audioFeatures.size()];
        int[] mode = new int[audioFeatures.size()];
        float[] speechiness = new float[audioFeatures.size()];
        float[] acousticness = new float[audioFeatures.size()];
        float[] instrumentalness = new float[audioFeatures.size()];
        float[] liveness = new float[audioFeatures.size()];
        float[] valence = new float[audioFeatures.size()];
        float[] tempo = new float[audioFeatures.size()];
        int[] duration = new int[audioFeatures.size()];
        int[] timeSignature = new int[audioFeatures.size()];

        for (int i = 0; i < audioFeatures.size(); i++) {
            danceability[i] = audioFeatures.get(i).getDanceability();
            energy[i] = audioFeatures.get(i).getEnergy();
            key[i] = audioFeatures.get(i).getKey();
            loudness[i] = audioFeatures.get(i).getLoudness();
            mode[i] = audioFeatures.get(i).getMode().getType();
            speechiness[i] = audioFeatures.get(i).getSpeechiness();
            acousticness[i] = audioFeatures.get(i).getAcousticness();
            instrumentalness[i] = audioFeatures.get(i).getInstrumentalness();
            liveness[i] = audioFeatures.get(i).getLiveness();
            valence[i] = audioFeatures.get(i).getValence();
            tempo[i] = audioFeatures.get(i).getTempo();
            duration[i] = audioFeatures.get(i).getDurationMs();
            timeSignature[i] = audioFeatures.get(i).getTimeSignature();
        }

        System.out.println("\ndanceability:");
        System.out.printf("\tAverage: %.3f\n", calculateAvg(danceability));
        System.out.printf("\tStandard Deviation: %.3f", calculateSD(danceability));

        System.out.println("\nenergy:");
        System.out.printf("\tAverage: %.3f\n", calculateAvg(energy));
        System.out.printf("\tStandard Deviation: %.3f", calculateSD(energy));

        System.out.println("\nkey:");
        System.out.printf("\tAverage: %.3f\n", calculateAvg(key));
        System.out.printf("\tStandard Deviation: %.3f", calculateSD(key));

        System.out.println("\nloudness:");
        System.out.printf("\tAverage: %.3f\n", calculateAvg(loudness));
        System.out.printf("\tStandard Deviation: %.3f", calculateSD(loudness));

        System.out.println("\nmode:");
        System.out.printf("\tMajor: %.2f%%\n", findOccurrences(mode, 1) * 100);
        System.out.printf("\tMinor: %.2f%%", findOccurrences(mode, 0) * 100);

        System.out.println("\nspeechiness:");
        System.out.printf("\tAverage: %.3f\n", calculateAvg(speechiness));
        System.out.printf("\tStandard Deviation: %.3f", calculateSD(speechiness));

        System.out.println("\nacousticness:");
        System.out.printf("\tAverage: %.3f\n", calculateAvg(acousticness));
        System.out.printf("\tStandard Deviation: %.3f", calculateSD(acousticness));

        System.out.println("\ninstrumentalness:");
        System.out.printf("\tAverage: %.3f\n", calculateAvg(instrumentalness));
        System.out.printf("\tStandard Deviation: %.3f", calculateSD(instrumentalness));

        System.out.println("\nliveness:");
        System.out.printf("\tAverage: %.3f\n", calculateAvg(liveness));
        System.out.printf("\tStandard Deviation: %.3f", calculateSD(liveness));

        System.out.println("\nvalence:");
        System.out.printf("\tAverage: %.3f\n", calculateAvg(valence));
        System.out.printf("\tStandard Deviation: %.3f", calculateSD(valence));

        System.out.println("\ntempo:");
        System.out.printf("\tAverage: %.3f\n", calculateAvg(tempo));
        System.out.printf("\tStandard Deviation: %.3f", calculateSD(tempo));

        System.out.println("\nduration:");
        System.out.printf("\tAverage: %.3f\n", calculateAvg(duration));
        System.out.printf("\tStandard Deviation: %.3f", calculateSD(duration));

        System.out.println("\ntime signature:");
        System.out.printf("\tAverage: %.3f\n", calculateAvg(timeSignature));
        System.out.printf("\tStandard Deviation: %.3f", calculateSD(timeSignature));
    }

    static double findOccurrences(int numArray[], int query) {
        int cnt = 0;

        for (int num : numArray) {
            if (num == query) cnt++;
        }

        return cnt / (double) numArray.length;
    }

    public static double calculateAvg(float numArray[]) {
        double sum = 0.0;
        for (double num : numArray) {
            sum += num;
        }
        return sum / numArray.length;
    }

    public static double calculateAvg(int numArray[]) {
        int sum = 0;
        for (int num : numArray) {
            sum += num;
        }
        return sum / (double) numArray.length;
    }

    public static double calculateSD(float numArray[]) {
        double sum = 0.0, standardDeviation = 0.0;
        int length = numArray.length;

        for (double num : numArray) {
            sum += num;
        }

        double mean = sum / length;

        for (double num : numArray) {
            standardDeviation += Math.pow(num - mean, 2);
        }

        return Math.sqrt(standardDeviation / length);
    }

    public static double calculateSD(int numArray[]) {
        int sum = 0, standardDeviation = 0;
        int length = numArray.length;

        for (int num : numArray) {
            sum += num;
        }

        int mean = sum / length;

        for (int num : numArray) {
            standardDeviation += Math.pow(num - mean, 2);
        }

        return Math.sqrt(standardDeviation / length);
    }

    static void getUserId(SpotifyApi spotifyApi) {
        GetCurrentUsersProfileRequest getCurrentUsersProfileRequest = spotifyApi.getCurrentUsersProfile().build();
        try {
            User user = getCurrentUsersProfileRequest.execute();
            userId = user.getId();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static String getPlaylistString(PlaylistSimplified playlist) {
        return String.format("%s by %s, with %d songs.", playlist.getName(), playlist.getOwner().getId(), playlist.getTracks().getTotal());
    }

    static String getAlbumString(Album album) {
        return String.format("%s by %s, released %s", album.getName(), getArtistString(album.getArtists()), album.getReleaseDate());
    }

    static String getPlaylistSimplifiedString(PlaylistSimplified playlist) {
        return String.format("%s by %s, with %d songs.", playlist.getName(), playlist.getOwner().getId(), playlist.getTracks().getTotal());
    }

    static String getAlbumSimplifiedString(AlbumSimplified album) {
        return String.format("%s by %s", album.getName(), getArtistString(album.getArtists()));
    }

    static String getArtistSimplifiedString(ArtistSimplified artist) {
        return String.format("%s", artist.getName());
    }

    static String getArtistString(ArtistSimplified[] artists) {
        String str = "";
        for (ArtistSimplified simplified : artists) {
            str += ", " + simplified.getName();
        }
        return str.substring(2);
    }

    static SpotifyApi authenticateSpotify() {
        SpotifyApi spotifyApi;
        //Authenticate
        File authFile = new File("auth");
        //Auth file exists
        if (authFile.exists()) {
            FileInputStream inputStream = null;
            try {
                //Read refresh token from file
                inputStream = new FileInputStream("auth");
                String refresh = new String(inputStream.readAllBytes());
                inputStream.close();
                //Create spotifyApi
                spotifyApi = new SpotifyApi.Builder()
                        .setClientId("7b8416f7032e449cbe0988517e03ba85")
                        .setClientSecret("e3e69e78268a4124aaf8e136903e1415")
                        .setRefreshToken(refresh)
                        .build();
                //Try to authenticate
                AuthorizationCodeRefreshRequest authRequest = spotifyApi.authorizationCodeRefresh().build();
                AuthorizationCodeCredentials authCred = authRequest.execute();
                spotifyApi.setAccessToken(authCred.getAccessToken());
                spotifyApi.setRefreshToken(authCred.getRefreshToken());
                //Empty contents of file
                PrintWriter writer = new PrintWriter("auth");
                writer.print("");
                writer.close();
                //Write new refresh token to file.
                FileOutputStream outputStream = new FileOutputStream("auth");
                outputStream.write(refresh.getBytes());
                outputStream.close();
                System.out.println("Authentication successful.");
                return spotifyApi;
            } catch (Exception e) {
                System.out.println("Authentication failed, please run program again.");
                new File("auth").delete();
                e.printStackTrace();
//                try {
//                    PrintWriter writer = new PrintWriter("auth");
//                    writer.print("");
//                    writer.close();
//                } catch (Exception ex) {
//                    ex.printStackTrace();
//                }
            }
        } else {
            URI redirectUri = SpotifyHttpManager.makeUri("https://bscholer.github.io/spotify-redirect/index.html");
            spotifyApi = new SpotifyApi.Builder()
                    .setClientId("7b8416f7032e449cbe0988517e03ba85")
                    .setClientSecret("e3e69e78268a4124aaf8e136903e1415")
                    .setRedirectUri(redirectUri)
                    .build();
            String refresh = authenticateSpotify(spotifyApi);
            FileOutputStream outputStream = null;
            try {
                PrintWriter writer = new PrintWriter("auth");
                writer.print("");
                writer.close();
                outputStream = new FileOutputStream("auth");
                outputStream.write(refresh.getBytes());
                outputStream.close();
                System.out.println("Authentication successful.");
                return spotifyApi;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    static String authenticateSpotify(SpotifyApi spotifyApi) {

        AuthorizationCodeUriRequest authorizationCodeUriRequest = spotifyApi.authorizationCodeUri()
                .scope("playlist-read-private,user-library-read,playlist-read-collaborative,playlist-modify-public," +
                        "playlist-modify-private,user-read-private,user-follow-read")
                .show_dialog(true)
                .build();
        URI uri = authorizationCodeUriRequest.execute();

        //User prompt stuff
        System.out.println("Please follow this link, and then copy and paste the code below.");
        System.out.println(uri.toString());
        Scanner scanner = new Scanner(System.in);
        System.out.print("Code: ");
        String code = scanner.nextLine();

        AuthorizationCodeRequest authorizationCodeRequest = spotifyApi.authorizationCode(code).build();
        try {
            AuthorizationCodeCredentials authorizationCodeCredentials = authorizationCodeRequest.execute();
            spotifyApi.setAccessToken(authorizationCodeCredentials.getAccessToken());
            spotifyApi.setRefreshToken(authorizationCodeCredentials.getRefreshToken());
            return authorizationCodeCredentials.getRefreshToken();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
