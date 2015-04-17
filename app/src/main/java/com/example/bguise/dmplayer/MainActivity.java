package com.example.bguise.dmplayer;

import com.example.bguise.developerkey.DeveloperKey;


import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.SearchResultSnippet;
import com.squareup.picasso.Picasso;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;


public class MainActivity extends ActionBarActivity {

    /** The request code when calling startActivityForResult to recover from an API service error. */
    private static final int RECOVERY_DIALOG_REQUEST = 1;

    /** Created to access YouTube Data API v3 */
    private static YouTube youtube;

    EditText userSearchText;
    ListView listView;

    private static final long MAX_VIDEOS_RETURNED = 20;
    private int numResults = 0;

    List<SearchResult> searchResultList;
    List<SearchResultSnippet> resultSnippets = new ArrayList<SearchResultSnippet>(20);




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = (ListView)findViewById(R.id.video_list);

        userSearchText = (EditText)findViewById(R.id.editText);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> av, View v, int pos,
                                    long id) {
                Intent intent = new Intent(getApplicationContext(), PlayActivity.class);
                // TODO: Move this around--should maintain searchResultList and use that to get snippets
                intent.putExtra("VIDEO_ID", searchResultList.get(pos).getId().getVideoId());
                startActivity(intent);
            }

        });
    }

    /** Called when the user clicks the Search button */
    public void search(View view) {
        // Hide keyboard on a search
        InputMethodManager inputManager = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);

        inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);

        // Latch will allow us to know when thread completes its task
        final CountDownLatch latch = new CountDownLatch(1);
        // Create new thread to handle the HTTP request, as Android will not allow this off the main thread
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                /** Construct query of YouTube and retrieve results */
                try {

                    // Setup youtube object to transfer HTTP request
                    youtube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), new HttpRequestInitializer() {
                        @Override
                        public void initialize(HttpRequest request) throws IOException {
                        }
                    }).setApplicationName("DMPlayer").build();

                    // Define API request for retrieving search results
                    YouTube.Search.List search = youtube.search().list("id,snippet");

                    // Setup developer key for request
                    search.setKey(DeveloperKey.DEVELOPER_KEY);

                    // Setup search query
                    String searchQuery = userSearchText.getText().toString();
                    search.setQ(searchQuery);

                    // Restrict search results to videos
                    search.setType("video");

                    // Select which fields we'd like returned
                    search.setFields("items(id/kind,id/videoId,snippet/title,snippet/thumbnails/default/url)");
                    search.setMaxResults(MAX_VIDEOS_RETURNED);

                    // Call YouTube Data API
                    SearchListResponse searchResponse = search.execute();

                    // Extract response into a list
                    searchResultList = new ArrayList<SearchResult>(searchResponse.getItems());
                    //searchResultList = searchResponse.getItems();

                    // Clear our old ArrayList
                    resultSnippets.clear();

                    Iterator<SearchResult> itr = searchResultList.iterator();
                    while (itr.hasNext()) {
                        resultSnippets.add(itr.next().getSnippet());
                    }

                    numResults = searchResultList.size();

                } catch (Throwable t) {
                    t.printStackTrace();
                }
                latch.countDown();
            }
        });

        //Start thread to execute the YouTube search
        thread.start();

        // Wait for thread to complete
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Update ListView with current results
        updateVideosFound();

        // Ensure our search() method is called, alert user
        CharSequence text = "Search returned " + numResults + " items";
        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
    }

    private void updateVideosFound() {
        // Array adapter: takes context of activity as the first parameter, the type of list view
        // as the second parameter, and your array as the third parameter.
        ArrayAdapter<SearchResultSnippet> arrAdapter = new ArrayAdapter<SearchResultSnippet>(this, R.layout.video_item, resultSnippets) {

            @Override
            public View getView(int pos, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = getLayoutInflater().inflate(R.layout.video_item, parent, false);
                }
                ImageView thumbnail = (ImageView) convertView.findViewById(R.id.video_thumbnail);
                TextView title = (TextView) convertView.findViewById(R.id.video_title);
                TextView description = (TextView) convertView.findViewById(R.id.video_description);

                SearchResultSnippet snippet = resultSnippets.get(pos);

                Picasso.with(getApplicationContext()).load(snippet.getThumbnails().getDefault().getUrl()).into(thumbnail);
                title.setText(snippet.getTitle());
                description.setText(snippet.getDescription());
                return convertView;
            }
        };

        listView.setAdapter(arrAdapter);
    }


    /** Called when user clicks the I'm Feeling Lucky button */
    public void getLucky(View view) {
        String rickRoll = "dQw4w9WgXcQ";
        Intent intent = new Intent(this, PlayActivity.class);
        intent.putExtra("VIDEO_ID", rickRoll);

        startActivity(intent);
    }



    /*
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Demo clickedDemo = (Demo) activities.get(position);

        Intent intent = new Intent();
        intent.setComponent(new ComponentName(getPackageName(), clickedDemo.className));
        startActivity(intent);
    }*/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
