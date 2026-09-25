package countdown;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EventStore {
    private final Path file;
    private final List<EventItem> items = new ArrayList<>();

    public EventStore(){this(defaultPath());}
    public EventStore(Path file){this.file=file;}

    public static Path defaultPath(){
        String appData=System.getenv("APPDATA");
        Path dir=(appData!=null&&!appData.trim().isEmpty())?Paths.get(appData,"CountdownApp"):Paths.get(System.getProperty("user.home"),".countdownapp");
        return dir.resolve("events.json");
    }

    public synchronized List<EventItem> items(){return items;}

    public synchronized void load(){
        items.clear();
        try{
            if(Files.exists(file)){
                String json=new String(Files.readAllBytes(file),StandardCharsets.UTF_8).trim();
                if(json.startsWith("[")&&json.endsWith("]")){
                    for(String part:JsonUtil.splitTopLevel(json.substring(1,json.length()-1))){
                        String t=part.trim();
                        if(t.startsWith("{")) items.add(EventItem.fromJson(t));
                    }
                }
            }
        }catch(Exception e){items.clear();}
        boolean changed = false;
        if(items.isEmpty()){
            items.add(seed("Valentine's Day",LocalDate.of(2027,2,14),
                    "A little countdown for a very special day.",true));
            items.add(seed("15 October - Focus Day",LocalDate.of(2024,10,15),
                    "It's 15 October, 12 o'clock! Open the app - your special day has arrived!",false));
            items.add(seed("27 March - Focus Day",LocalDate.of(2003,3,27),
                    "It's 27 March, 12 o'clock! Open the app - your special day has arrived!",false));
            changed = true;
        }
        boolean hasValentine = false;
        for(EventItem e : items) {
            if(e.title != null && e.title.toLowerCase().contains("valentine")) {
                hasValentine = true;
                break;
            }
        }
        if(!hasValentine) {
            items.add(0, seed("Valentine's Day", LocalDate.of(2027,2,14),
                    "A little countdown for a very special day.", true));
            changed = true;
        }
        if(changed) save();
    }

    private static EventItem seed(String title,LocalDate date,String message,boolean featured){
        EventItem e=new EventItem(); e.title=title;e.date=date;e.message=message;e.featured=featured;
        e.repeatYearly=true;e.soundEnabled=true;return e;
    }

    public synchronized void save(){
        try{
            Files.createDirectories(file.getParent());
            StringBuilder sb=new StringBuilder("[\n");
            for(int i=0;i<items.size();i++){sb.append("  ").append(items.get(i).toJson());if(i+1<items.size())sb.append(",");sb.append("\n");}
            sb.append("]\n");
            Files.write(file,sb.toString().getBytes(StandardCharsets.UTF_8));
        }catch(IOException ignored){}
    }

    public synchronized void addOrUpdate(EventItem item){
        for(EventItem e:items) if(e.id.equals(item.id)){
            e.title=item.title;e.date=item.date;e.message=item.message;e.secretMessage=item.secretMessage;
            e.secretEnabled=item.secretEnabled;e.featured=item.featured;e.repeatYearly=item.repeatYearly;e.soundEnabled=item.soundEnabled;
            save();return;
        }
        items.add(item);save();
    }

    public synchronized void delete(String id){items.removeIf(e->e.id.equals(id));save();}
    public synchronized EventItem byId(String id){for(EventItem e:items)if(e.id.equals(id))return e;return null;}

    public synchronized List<EventItem> sortedByNext(LocalDate today){
        List<EventItem> copy=new ArrayList<>(items);
        copy.sort(Comparator.comparing((EventItem e)->!e.featured).thenComparing(e->e.nextOccurrence(today)));
        return copy;
    }
}
