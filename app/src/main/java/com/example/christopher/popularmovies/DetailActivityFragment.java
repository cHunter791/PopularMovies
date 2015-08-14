package com.example.christopher.popularmovies;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;


/**
 * A placeholder fragment containing a simple view.
 */
public class DetailActivityFragment extends Fragment {
    private View view;

    public DetailActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Intent intent = getActivity().getIntent();
        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
        if (intent != null && intent.hasExtra(Intent.EXTRA_TEXT)) {
            String movieID = intent.getStringExtra(Intent.EXTRA_TEXT);
            getDetails(movieID);
        }
        view = rootView;

        return rootView;
    }

    private void getDetails(String movieID) {
        new FetchMovieDetails().execute(movieID);
    }

    public class FetchMovieDetails extends AsyncTask<String, Void, String[]> {

        private final String LOG_TAG = FetchMovieDetails.class.getSimpleName();

        public String[] getMovieDetailsFromJSON(String json) throws JSONException, MalformedURLException {
            final String MOV_TITLE = "original_title";
            final String MOV_POSTER = "poster_path";
            final String MOV_RELEASE = "release_date";
            final String MOV_RATING = "vote_average";
            final String MOV_PLOT = "overview";

            JSONObject movieJson = new JSONObject(json);
            String movieTitle = movieJson.getString(MOV_TITLE);
            String moviePoster = movieJson.getString(MOV_POSTER);
            String movieRelease = movieJson.getString(MOV_RELEASE);
            String movieRating = movieJson.getString(MOV_RATING);
            String moviePlot = movieJson.getString(MOV_PLOT);

            String[] resultsStr = new String[5];
            resultsStr[0] = movieTitle;
            resultsStr[1] = convertURL(moviePoster);
            resultsStr[2] = movieRelease;
            resultsStr[3] = movieRating;
            resultsStr[4] = moviePlot;

            for (String str : resultsStr) {
                Log.d("MovieDetails", "Detail: " + str);
            }
            return resultsStr;
        }

        public String convertURL(String path) throws MalformedURLException, JSONException {
            final String BASE_URL = "http://image.tmdb.org/t/p";
            final String SIZE = "/w342";

            String resultStr;
            Uri builtUri = null;

            builtUri = Uri.parse(BASE_URL).buildUpon()
                    .appendEncodedPath(SIZE)
                    .appendEncodedPath(path)
                    .build();
            URL url = new URL(builtUri.toString());
            resultStr = url.toString();

            return resultStr;
        }

        @Override
        protected String[] doInBackground(String... params) {
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            String movieJsonStr = null;
            String[] movieDetails = new String[4];

            try {
                final String MOVIEDB_BASE_URL = "http://api.themoviedb.org/3/movie";
                final String API_PARAM = "api_key";
                final String KEY_PARAM = "95e7bb147d8f59a79ec0058f8a4014ef";

                Uri builtUri = null;

                builtUri = Uri.parse(MOVIEDB_BASE_URL).buildUpon()
                        .appendPath(params[0])
                        .appendQueryParameter(API_PARAM, KEY_PARAM)
                        .build();

                URL url = new URL(builtUri.toString());

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }

                movieJsonStr = buffer.toString();
                movieDetails = getMovieDetailsFromJSON(movieJsonStr);

            } catch (MalformedURLException e) {
                Log.e(LOG_TAG, "URL Error " + e);
            } catch (ProtocolException e) {
                Log.e(LOG_TAG, "Protocol Error " + e);
            } catch (IOException e) {
                Log.e(LOG_TAG, "IO Error " + e);
            } catch (JSONException e) {
                Log.e(LOG_TAG, "JSON Error " + e);
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }
            return movieDetails;
        }

        @Override
        protected void onPostExecute(String[] strings) {
            TextView titleText = (TextView) view.findViewById(R.id.titleText);
            titleText.setText(strings[0]);

            ImageView posterImage = (ImageView) view.findViewById(R.id.posterImage);
            Picasso.with(getActivity()).load(strings[1]).into(posterImage);

            TextView releaseText = (TextView) view.findViewById(R.id.releaseText);
            releaseText.setText(strings[2]);

            TextView ratingText = (TextView) view.findViewById(R.id.ratingText);
            ratingText.setText(strings[3]);

            TextView plotText = (TextView) view.findViewById(R.id.plotText);
            plotText.setText(strings[4]);
        }
    }
}
