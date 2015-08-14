package com.example.christopher.popularmovies;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {
    String[] ids;
    private List<String> urls = new ArrayList<String>();
    private ImageAdapter mPosterAdapter;

    @Override
    public void onStart() {
        super.onStart();
        updateMovieURLs();
    }

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);


        mPosterAdapter = new ImageAdapter(getActivity());

        GridView gridView = (GridView) rootView.findViewById(R.id.gridview);
        gridView.setAdapter(mPosterAdapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String movieID = ids[position];
                Intent detailIntent = new Intent(getActivity(), DetailActivity.class)
                        .putExtra(Intent.EXTRA_TEXT, movieID);
                startActivity(detailIntent);
            }
        });
        return rootView;
    }

    public void updateMovieURLs() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String sort = prefs.getString("sort", "Popularity");
        new FetchMovieID().execute(sort);
    }

    public class FetchMovieID extends AsyncTask<String, Void, Void> {

        private final String LOG_TAG = FetchMovieID.class.getSimpleName();

        public void getMovieIDFromJson(String json) throws JSONException{
            final String MOV_RESULTS = "results";
            final String MOV_ID = "id";

            JSONObject movieJson = new JSONObject(json);
            JSONArray movieArray = movieJson.getJSONArray(MOV_RESULTS);

            String[] resultsStr = new String[movieArray.length()];
            for(int i = 0; i < movieArray.length(); i++) {
                String id;

                JSONObject movieObj = movieArray.getJSONObject(i);
                id = movieObj.getString(MOV_ID);

                resultsStr[i] = id;
            }

            ids = resultsStr;
        }

        public String[] convertURLs(String json) throws MalformedURLException, JSONException {
            final String MOV_RESULTS = "results";
            final String MOV_POSTER = "poster_path";

            JSONObject movieJson = new JSONObject(json);
            JSONArray movieArray = movieJson.getJSONArray(MOV_RESULTS);

            String[] posterStr = new String[movieArray.length()];
            for(int i = 0; i < movieArray.length(); i++) {
                String id;

                JSONObject movieObj = movieArray.getJSONObject(i);
                id = movieObj.getString(MOV_POSTER);

                posterStr[i] = id;
            }

            final String BASE_URL = "http://image.tmdb.org/t/p";
            final String SIZE = "/w342";

            String[] resultStr = new String[posterStr.length];

            Uri builtUri = null;

            for(int i = 0; i < posterStr.length; i++) {
                builtUri = Uri.parse(BASE_URL).buildUpon()
                        .appendEncodedPath(SIZE)
                        .appendEncodedPath(posterStr[i])
                        .build();
                URL url = new URL(builtUri.toString());
                resultStr[i] = url.toString();
            }

            return resultStr;
        }

        @Override
        protected Void doInBackground(String... params) {
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            String movieJsonStr = null;

            try {
                final String MOVIEDB_BASE_URL = "http://api.themoviedb.org/3/discover/movie?";
                final String SORT_PARAM = "sort_by";
                final String POPULAR_PARAM = "popularity.desc";
                final String RATE_PARAM = "vote_average.desc";
                final String API_PARAM = "api_key";
                final String KEY_PARAM = "95e7bb147d8f59a79ec0058f8a4014ef";

                Uri builtUri = null;
                Log.d(LOG_TAG, "Sort by " + params[0]);
                if (params[0].equals("Popularity")) {
                    builtUri = Uri.parse(MOVIEDB_BASE_URL).buildUpon()
                            .appendQueryParameter(SORT_PARAM, POPULAR_PARAM)
                            .appendQueryParameter(API_PARAM, KEY_PARAM)
                            .build();
                } else if (params[0].equals("Rating")) {
                    builtUri = Uri.parse(MOVIEDB_BASE_URL).buildUpon()
                            .appendQueryParameter(SORT_PARAM, RATE_PARAM)
                            .appendQueryParameter(API_PARAM, KEY_PARAM)
                            .build();
                }

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
                getMovieIDFromJson(movieJsonStr);

                urls.clear();
                if (urls.isEmpty()) {
                    for (String str : convertURLs(movieJsonStr)) {
                        urls.add(str);
                    }
                }

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
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mPosterAdapter.notifyDataSetChanged();
        }
    }

    public class ImageAdapter extends BaseAdapter {
        private Context context;

        public ImageAdapter(Context c) {
            context = c;
        }

        @Override
        public int getCount() {
            return urls.size();
        }

        //---returns the ID of an item---
        public String getItem(int position) {
            return urls.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        //---returns an ImageView view---
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView = (ImageView) convertView;
            if (imageView == null) {
                imageView = new ImageView(context);
            }

            String currentURL = getItem(position);

            Picasso.with(context)
                    .load(currentURL)
                    .placeholder(R.mipmap.ic_placeholder)
                    .error(R.mipmap.ic_error)
                    .into(imageView);

            return imageView;
        }
    }
}
