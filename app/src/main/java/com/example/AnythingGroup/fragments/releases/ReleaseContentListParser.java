package com.example.AnythingGroup.fragments.releases;

import android.graphics.Bitmap;

import com.example.AnythingGroup.Network;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

public class ReleaseContentListParser {
    public static ArrayList<ContentListItem> parse(String url) throws IOException {
        ArrayList<ContentListItem> items = new ArrayList<>();

        Document document = Network.get(url);

        Elements release_list = document.getElementsByClass("content_list_item");

        for (Element release_item: release_list) {
            ContentListItem item = new ContentListItem();

            Element title = release_item.getElementsByClass("ngr_title").get(0);
            Element title_inner = title.getElementsByTag("a").get(0);
            item.reference = title_inner.attributes().get("href");
            item.title = title_inner.html();

            Element photo = release_item.getElementsByClass("photo").get(0);

            Element image_container = photo.getElementsByTag("a").get(1);
            Element image = image_container.getElementsByTag("img").get(0);
            String image_reference = image.attributes().get("src");
            item.image = Network.getImageFromURL(image_reference);

            Element state = photo.getElementsByClass("ngr_photo_footer").get(0);
            Element state_inner = state.getElementsByTag("img").get(0);
            String state_reference = state_inner.attributes().get("src");
            int state_name_start = state_reference.lastIndexOf("/");
            String state_name = state_reference.substring(state_name_start + 1);

            switch (state_name) {
                case "in_progress.png":
                    item.state = ContentListItem.ReleaseState.WorkInProgress;
                    break;
                case "ongoing.png":
                    item.state = ContentListItem.ReleaseState.Ongoing;
                    break;
                case "complete.png":
                    item.state = ContentListItem.ReleaseState.Complete;
                    break;
                case "freeze.png":
                    item.state = ContentListItem.ReleaseState.Stopped;
                    break;
                default:
                    break;
            }

            items.add(item);
        }

        return items;
    }

    public static class ContentListItem{
        public String title;
        public String reference;
        public Bitmap image;
        public ReleaseState state;

        public enum ReleaseState{
            Ongoing,
            WorkInProgress,
            Complete,
            Stopped,
        }
    }

    public static class ContentListState {
        public UUID workerId = null;

        public ContentState state = ContentState.None;

        public int pagesLoaded = 0;
    }

    public static class ContentList<T> extends ArrayList<T>{
        public ContentListState state = new ContentListState();
    }

    public enum ContentState{
        None,
        Loading,
        Loaded,
        AllLoaded
    }
}