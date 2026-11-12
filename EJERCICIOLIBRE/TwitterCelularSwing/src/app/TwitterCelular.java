package app;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;

/** Simulador de X/Twitter en Java Swing puro. Sin servicios externos. */
public class TwitterCelular {
    static final Color BG = new Color(0, 0, 0), FG = new Color(239,243,244), MUTED = new Color(113,118,123),
        BORDER = new Color(43,47,51), BLUE = new Color(29,155,240), INPUT = new Color(32,35,39),
        PINK = new Color(249,24,128);
    static final Font REG = font(Font.PLAIN, 13), SMALL = font(Font.PLAIN, 11), BOLD = font(Font.BOLD, 14), TITLE = font(Font.BOLD, 17);
    static Font font(int style, int size) { return new Font("Segoe UI", style, size); }
    JFrame window; JPanel phone, headerHolder, pages, navHolder; CardLayout cards;
    String current = "feed"; Post selected;
    java.util.List<Post> posts = new ArrayList<>();
    int nextId = 4; JTextField searchField;
    boolean refreshQueued = false;
    final File saveFile = new File(System.getProperty("user.home"), ".twitter_celular_swing.dat");

    static class Reply implements Serializable {
        private static final long serialVersionUID = 1L;
        String user, text;
        Reply(String u,String t) { user=u; text=t; }
    }
    static class Post implements Serializable {
        private static final long serialVersionUID = 1L;
        int id, likes, reposts, views;
        String user, handle, text, time;
        boolean liked, reposted, bookmarked, media;
        List<Reply> replies = new ArrayList<>();
        Post(int id,String u,String h,String t,boolean m,int l,int r,int v) {
            this.id=id;user=u;handle=h;text=t;media=m;likes=l;reposts=r;views=v;time="1d";
        }
    }
    public static void main(String[] args) {
        System.setProperty("sun.java2d.uiScale.enabled", "true");
        SwingUtilities.invokeLater(() -> new TwitterCelular().start());
    }
    void start() {
        load();
        window = new JFrame("X · Simulador interactivo | Java Swing");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.getContentPane().setBackground(new Color(22,25,29));
        window.setLayout(new GridBagLayout());
        phone = new JPanel(new BorderLayout());
        phone.setBackground(BG);
        phone.setPreferredSize(new Dimension(404,780));
        phone.setBorder(new CompoundBorder(new LineBorder(new Color(71,77,83), 4, true), new EmptyBorder(5,3,5,3)));
        window.add(phone);
        headerHolder = new JPanel(new BorderLayout()); headerHolder.setBackground(BG);
        pages = new JPanel(); cards = new CardLayout(); pages.setLayout(cards); pages.setBackground(BG);
        navHolder = new JPanel(new BorderLayout()); navHolder.setBackground(BG);
        phone.add(headerHolder, BorderLayout.NORTH); phone.add(pages,BorderLayout.CENTER); phone.add(navHolder,BorderLayout.SOUTH);
        window.setSize(520,890); window.setMinimumSize(new Dimension(460,850));
        window.setLocationRelativeTo(null);
        show("feed"); window.setVisible(true);
    }
    void load() {
        if(saveFile.isFile()) try(ObjectInputStream in=new ObjectInputStream(new FileInputStream(saveFile))) {
            Object val=in.readObject(); if(val instanceof ArrayList<?>) {
                for(Object o:(ArrayList<?>)val) if(o instanceof Post) posts.add((Post)o);
                for(Post p:posts) nextId=Math.max(nextId,p.id+1);
            }
        } catch(Exception ignored) {}
        if(posts.isEmpty()) {
            Post a=new Post(1,"Cami con Alma | Hábitos & Bienestar", "@Camiconalma", "Durante toda mi vida, pensé que era una cabeza con hombros.",true,12600,206,3700000);
            a.replies.add(new Reply("Sofía · @sofi","¡Y al final todos tenemos una historia que contar! 🥹"));
            a.replies.add(new Reply("Marco · @marco","JAJAJA yo también pensé eso cuando era pequeño."));
            a.replies.add(new Reply("Luna · @luna","Me encanta cómo una frase simple cambia la perspectiva."));
            Post b=new Post(2,"Cami con Alma | Hábitos & Bienestar","@Camiconalma","Un rumor: Kobe es increíblemente bueno. No es de extrañar si te dijeron que era IA. Como era de esperar de un atleta olímpico. ✨",false,218,19,7800);
            Post c=new Post(3,"Tecnología hoy","@techhoy","Aprender algo nuevo cada día es una excelente manera de seguir creciendo. ¿Qué aprendiste hoy? 💙",false,89,12,2500);
            posts.add(a); posts.add(b); posts.add(c);
        }
    }
    void save() { try(ObjectOutputStream out=new ObjectOutputStream(new FileOutputStream(saveFile))) {out.writeObject(new ArrayList<>(posts));} catch(Exception ex){System.err.println("No se pudo guardar: "+ex.getMessage());} }

    void show(String screen) {
        current=screen;
        headerHolder.removeAll(); pages.removeAll(); navHolder.removeAll();
        switch(screen) {
            case "feed": headerHolder.add(feedHeader()); pages.add(scroller(feedContent()),"one"); break;
            case "focus": headerHolder.add(topBar("Publicación", "detail")); pages.add(scroller(focusContent()),"one"); break;
            case "detail": headerHolder.add(topBar("Post", "feed")); pages.add(scroller(detailContent()),"one"); break;
            case "replies": headerHolder.add(topBar("Respuestas", "detail")); pages.add(scroller(repliesContent()),"one"); break;
            case "search": headerHolder.add(topBar("Explorar", "feed")); pages.add(searchContent(),"one"); break;
            case "notifications": headerHolder.add(topBar("Notificaciones", "feed")); pages.add(scroller(notificationsContent()),"one"); break;
            case "profile": headerHolder.add(topBar("Tu perfil", "feed")); pages.add(scroller(profileContent()),"one"); break;
            case "bookmarks": headerHolder.add(topBar("Guardados", "feed")); pages.add(scroller(bookmarkContent()),"one"); break;
            default: headerHolder.add(feedHeader()); pages.add(scroller(feedContent()),"one");
        }
        if(screen.equals("replies")) navHolder.add(replyComposer(),BorderLayout.CENTER);
        else if(screen.equals("detail")) navHolder.add(replyShortcut(),BorderLayout.CENTER);
        else navHolder.add(bottomNav(), BorderLayout.CENTER);
        cards.show(pages,"one");
        phone.revalidate(); phone.repaint();
    }
    JPanel feedHeader() {
        JPanel all=column();
        JPanel top=new JPanel(new BorderLayout()); top.setBackground(BG); top.setBorder(new EmptyBorder(12,16,12,16));
        JButton avatar=avatarButton("T", new Color(91,64,108),38); avatar.addActionListener(e->show("profile")); top.add(avatar,BorderLayout.WEST);
        JLabel x=new JLabel("𝕏",SwingConstants.CENTER); x.setForeground(FG); x.setFont(font(Font.BOLD,29)); top.add(x,BorderLayout.CENTER);
        JButton more=button("⋯",18,MUTED); more.addActionListener(e->popup(more,new String[]{"Guardados","Mi perfil","Publicar"},new Runnable[]{()->show("bookmarks"),()->show("profile"),this::compose}));top.add(more,BorderLayout.EAST); all.add(top);
        JPanel tabs=new JPanel(new GridLayout(1,2));tabs.setBackground(BG);tabs.setMaximumSize(new Dimension(Integer.MAX_VALUE,42));
        tabs.add(tab("Para ti",true));tabs.add(tab("Siguiendo",false)); all.add(tabs); all.add(line());return all;
    }
    JButton tab(String t,boolean active) {JButton b=button(t,13,active?FG:MUTED);b.setFont(BOLD);b.addActionListener(e->{ if(!active) alert("En esta demostración, el feed 'Siguiendo' usa las publicaciones locales.");});return b;}
    JPanel topBar(String label,String back) {
        JPanel bar=new JPanel(new BorderLayout());bar.setBackground(BG);bar.setBorder(new EmptyBorder(10,9,10,12));
        JButton backBtn=iconButton("back",FG,()->show(back));backBtn.setPreferredSize(new Dimension(42,36));bar.add(backBtn,BorderLayout.WEST);
        JLabel title=new JLabel(label);title.setForeground(FG);title.setFont(TITLE);bar.add(title,BorderLayout.CENTER);
        JButton menu=iconButton("more",FG,()->popup(null,new String[]{"Inicio","Guardados","Perfil"},new Runnable[]{()->show("feed"),()->show("bookmarks"),()->show("profile")}));bar.add(menu,BorderLayout.EAST);
        return bar;
    }
    JPanel bottomNav() {
        JPanel outer=column();outer.add(line());JPanel bar=new JPanel(new GridLayout(1,5));bar.setBackground(BG);bar.setPreferredSize(new Dimension(390,58));
        bar.add(iconButton("home",FG,()->show("feed")));bar.add(iconButton("search",FG,()->show("search")));
        bar.add(iconButton("bell",FG,()->show("notifications")));bar.add(iconButton("bookmark",FG,()->show("bookmarks")));
        bar.add(iconButton("profile",FG,()->show("profile")));outer.add(bar); return outer;
    }
    JPanel feedContent() {
        JPanel p=column();p.add(Box.createVerticalStrut(25));for(Post post:new ArrayList<>(posts)) {p.add(feedPost(post));p.add(line());}
        JPanel wrap=p;
        JButton fab=new RoundedButton("+  Publicar",BLUE,Color.WHITE,22);
        fab.setAlignmentX(Component.RIGHT_ALIGNMENT);fab.setMaximumSize(new Dimension(130,43));
        fab.addActionListener(e->compose());
        JPanel holder=new JPanel(new FlowLayout(FlowLayout.RIGHT,17,10));holder.setBackground(BG);holder.add(fab);wrap.add(holder);
        return wrap;
    }
    JPanel feedPost(Post post) {
        int textHeight = post.text.length()>125 ? 82 : post.text.length()>72 ? 65 : 46;
        int mediaStart = 59 + textHeight;
        int actionsY = post.media ? mediaStart+205 : mediaStart+5;
        int h = actionsY+48;
        JPanel whole = new JPanel(null);
        whole.setOpaque(true);whole.setBackground(BG);
        whole.setPreferredSize(new Dimension(370,h));
        whole.setMinimumSize(new Dimension(370,h));
        whole.setMaximumSize(new Dimension(370,h));
        whole.setAlignmentX(Component.RIGHT_ALIGNMENT);
        JPanel head=postHeader(post,true);
        head.setBounds(13,10,344,43);whole.add(head);
        JTextArea text=bodyText(post.text,14,FG);
        text.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        text.addMouseListener(new MouseAdapter(){public void mouseClicked(MouseEvent e){selected=post;show("focus");}});
        text.setBounds(61,59,282,textHeight);whole.add(text);
        if(post.media) {
            MediaPanel media=new MediaPanel();media.setBounds(61,mediaStart,282,198);
            media.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            media.addMouseListener(new MouseAdapter(){public void mouseClicked(MouseEvent e){selected=post;show("focus");}});
            whole.add(media);
        }
        JPanel act=actions(post);act.setBounds(48,actionsY,312,40);whole.add(act);
        return whole;
    }
    JPanel postHeader(Post post, boolean clickable) {
        JPanel row=new JPanel(new BorderLayout(8,0));row.setBackground(BG);row.setMaximumSize(new Dimension(Integer.MAX_VALUE,47));
        row.add(avatarButton(post.user.startsWith("Cami")?"C":post.user.substring(0,1), post.user.startsWith("Cami")?new Color(124,70,55):new Color(65,94,131),40),BorderLayout.WEST);
        JPanel names=column(); JLabel name=label(post.user,FG,Font.BOLD,12);name.setText(ellipsize(post.user,35));names.add(name);
        JLabel handle=label(post.handle+" · "+post.time,MUTED,Font.PLAIN,11);names.add(handle);row.add(names,BorderLayout.CENTER);
        JButton dots=iconButton("more",MUTED,()->postMenu(post));dots.setPreferredSize(new Dimension(32,30));row.add(dots,BorderLayout.EAST);
        if(clickable) {
            MouseAdapter m=new MouseAdapter(){public void mouseClicked(MouseEvent e){selected=post;show("focus");}};
            names.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));names.addMouseListener(m);
        }
        return row;
    }
    JPanel focusContent() {
        if(selected==null)selected=posts.get(0);
        JPanel p=column();p.add(feedPost(selected));
        JPanel controls=new JPanel(new FlowLayout(FlowLayout.LEFT,12,8));controls.setBackground(BG);
        RoundedButton open=new RoundedButton("Ver detalles del post",INPUT,FG,19);
        open.addActionListener(e->show("detail"));controls.add(open);
        RoundedButton reply=new RoundedButton("Responder",BLUE,Color.WHITE,19);reply.addActionListener(e->show("replies"));controls.add(reply);
        p.add(controls);p.add(line());p.add(labelPad("Vista compacta de la publicación",MUTED,12));return p;
    }
    JPanel detailContent() {
        if(selected==null)selected=posts.get(0);Post post=selected;
        JPanel p=column();p.setBorder(new EmptyBorder(15,13,6,13));p.add(postHeader(post,false));p.add(Box.createVerticalStrut(13));
        JTextArea body=bodyText(post.text,16,FG);p.add(body);p.add(Box.createVerticalStrut(15));
        if(post.media) {MediaPanel pic=new MediaPanel();pic.setPreferredSize(new Dimension(358,275));pic.setMaximumSize(new Dimension(358,275));p.add(pic);p.add(Box.createVerticalStrut(14));}
        p.add(label("9:13 a. m. · 3/11/26 · "+compact(post.views)+" visualizaciones",MUTED,Font.PLAIN,11));p.add(Box.createVerticalStrut(12));p.add(line());
        JLabel counts=label("  "+compact(post.reposts)+" Reposts         "+compact(post.likes)+" Me gusta",FG,Font.PLAIN,12);
        counts.setBorder(new EmptyBorder(11,0,11,0));p.add(counts);p.add(line());p.add(actions(post));p.add(line());
        JPanel relevant=new JPanel(new BorderLayout());relevant.setBackground(BG);relevant.setBorder(new EmptyBorder(12,0,10,0));
        relevant.add(label("Relevante ▾",FG,Font.BOLD,12),BorderLayout.WEST);
        JButton see=button("Ver respuestas",12,BLUE);see.addActionListener(e->show("replies"));relevant.add(see,BorderLayout.EAST);p.add(relevant);
        for(Reply r:post.replies) {p.add(comment(r));p.add(line());}
        JButton more=button("Ver todas las respuestas  →",13,BLUE);more.addActionListener(e->show("replies"));p.add(more);return p;
    }
    JPanel repliesContent() {
        if(selected==null)selected=posts.get(0);JPanel p=column();
        p.add(feedPost(selected));p.add(line());
        JLabel h=label("Respuestas · "+selected.replies.size(),FG,Font.BOLD,14);
        h.setBorder(new EmptyBorder(13,14,12,10));p.add(h);
        for(Reply r:selected.replies){p.add(comment(r));p.add(line());}
        if(selected.replies.isEmpty())p.add(labelPad("Todavía no hay respuestas. ¡Sé el primero!",MUTED,13));
        return p;
    }
    JPanel comment(Reply r) {
        JPanel whole=column();whole.setBorder(new EmptyBorder(12,13,12,13));
        JPanel first=new JPanel(new BorderLayout(8,0));first.setBackground(BG);
        first.add(avatarButton(r.user.substring(0,1),new Color(95,78,108),32),BorderLayout.WEST);
        JPanel info=column();info.add(label(r.user,FG,Font.BOLD,12));info.add(label("Respondiendo a @Camiconalma",BLUE,Font.PLAIN,11));first.add(info,BorderLayout.CENTER);
        whole.add(first);JTextArea text=bodyText(r.text,12,FG);whole.add(indent(text,40,5));
        JPanel acts=new JPanel(new FlowLayout(FlowLayout.LEFT,15,3));acts.setBackground(BG);
        acts.add(iconButton("reply",MUTED,()->show("replies")));acts.add(iconButton("heart",MUTED,()->alert("Me gusta registrado en esta demostración.")));
        whole.add(indent(acts,35,3));return whole;
    }
    JPanel replyShortcut(){JPanel p=column();p.add(line());JButton b=new RoundedButton("Postea tu respuesta…",INPUT,MUTED,23);b.setAlignmentX(Component.LEFT_ALIGNMENT);b.setMaximumSize(new Dimension(Integer.MAX_VALUE,42));b.addActionListener(e->show("replies"));p.add(padded(b,7,11));return p;}
    JTextArea replyText;
    JPanel replyComposer(){
        JPanel p=column();p.add(line());
        JPanel row=new JPanel(new BorderLayout(8,6));row.setBackground(BG);row.setBorder(new EmptyBorder(8,10,7,10));
        replyText=new JTextArea(2,23);styleTextArea(replyText,13);replyText.setBackground(INPUT);
        replyText.setBorder(new CompoundBorder(new LineBorder(BORDER,1,true),new EmptyBorder(8,10,8,10)));
        row.add(new JScrollPane(replyText,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER),BorderLayout.CENTER);
        RoundedButton send=new RoundedButton("Responder",Color.WHITE,Color.BLACK,20);send.setPreferredSize(new Dimension(95,39));
        send.addActionListener(e->{String message=replyText.getText().trim();if(message.isEmpty()){alert("Escribe una respuesta primero.");return;}selected.replies.add(new Reply("Tú · @usuario",message));save();show("replies");});
        row.add(send,BorderLayout.EAST);p.add(row);return p;
    }
    JPanel actions(Post post) {
        JPanel acts=new JPanel(new GridLayout(1,5,2,0));acts.setBackground(BG);acts.setMaximumSize(new Dimension(Integer.MAX_VALUE,39));
        acts.add(action("reply",compact(post.replies.size()),MUTED,()->{selected=post;show("replies");}));
        acts.add(action("repost",compact(post.reposts),post.reposted?new Color(0,186,124):MUTED,()->{post.reposted=!post.reposted;post.reposts+=post.reposted?1:-1;save();show(current);}));
        acts.add(action("heart",compact(post.likes),post.liked?PINK:MUTED,()->{post.liked=!post.liked;post.likes+=post.liked?1:-1;save();show(current);}));
        acts.add(action("chart",compact(post.views),MUTED,()->{selected=post;show("detail");}));
        acts.add(action("bookmark", "",post.bookmarked?BLUE:MUTED,()->{post.bookmarked=!post.bookmarked;save();show(current);}));
        return acts;
    }
    JButton action(String icon,String number,Color color,Runnable action) {
        JButton btn=new JButton(number,new Glyph(icon,color,16));btn.setHorizontalAlignment(SwingConstants.LEFT);btn.setIconTextGap(5);
        btn.setFont(SMALL);btn.setForeground(color);clearButton(btn);btn.addActionListener(e->action.run());return btn;
    }
    void postMenu(Post post) {
        JPopupMenu menu=new JPopupMenu();menu.setBackground(INPUT);
        JMenuItem bookmark=new JMenuItem(post.bookmarked?"Quitar de guardados":"Guardar publicación");
        bookmark.addActionListener(e->{post.bookmarked=!post.bookmarked;save();show(current);});menu.add(bookmark);
        JMenuItem reply=new JMenuItem("Responder");reply.addActionListener(e->{selected=post;show("replies");});menu.add(reply);
        if(post.handle.equals("@usuario")) {JMenuItem del=new JMenuItem("Eliminar mi publicación");del.addActionListener(e->{if(JOptionPane.showConfirmDialog(window,"¿Eliminar la publicación?","Confirmar",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION){posts.remove(post);save();show("feed");}});menu.add(del);}
        menu.show(phone,80,140);
    }
    void popup(Component anchor,String[] labels,Runnable[] tasks) {
        JPopupMenu pm=new JPopupMenu();for(int i=0;i<labels.length;i++){JMenuItem item=new JMenuItem(labels[i]);final Runnable r=tasks[i];item.addActionListener(e->r.run());pm.add(item);}pm.show(phone,210,50);
    }
    void compose() {
        JTextArea area=new JTextArea(7,25);styleTextArea(area,14);area.setBackground(INPUT);
        JPanel panel=column();panel.setBorder(new EmptyBorder(8,8,8,8));
        panel.add(label("¿Qué está pasando?",FG,Font.BOLD,15));panel.add(Box.createVerticalStrut(10));panel.add(new JScrollPane(area));
        int result=JOptionPane.showConfirmDialog(window,panel,"Nuevo post",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
        if(result==JOptionPane.OK_OPTION){String t=area.getText().trim();if(t.isEmpty()){alert("No puedes publicar un texto vacío.");return;}if(t.length()>280){alert("Máximo 280 caracteres por publicación.");return;}
            Post newPost=new Post(nextId++,"Tú","@usuario",t,false,0,0,0);newPost.time="ahora";posts.add(0,newPost);save();show("feed");}
    }
    JPanel searchContent() {
        JPanel page=new JPanel(new BorderLayout());page.setBackground(BG);
        JPanel head=new JPanel(new BorderLayout());head.setBackground(BG);head.setBorder(new EmptyBorder(10,12,10,12));
        searchField=new JTextField();searchField.setFont(REG);searchField.setForeground(FG);searchField.setBackground(INPUT);searchField.setCaretColor(FG);
        searchField.setBorder(new CompoundBorder(new LineBorder(BORDER,1,true),new EmptyBorder(8,12,8,12)));
        head.add(searchField,BorderLayout.CENTER);JButton search=button("Buscar",13,BLUE);head.add(search,BorderLayout.EAST);page.add(head,BorderLayout.NORTH);
        JPanel results=column();page.add(scroller(results),BorderLayout.CENTER);
        Runnable find=()->{results.removeAll();String q=searchField.getText().trim().toLowerCase(Locale.ROOT);
            int found=0;for(Post p:posts)if(q.isEmpty()||p.text.toLowerCase(Locale.ROOT).contains(q)||p.user.toLowerCase(Locale.ROOT).contains(q)) {results.add(feedPost(p));results.add(line());found++;}
            if(found==0)results.add(labelPad("No se encontraron publicaciones.",MUTED,14));results.revalidate();results.repaint();};
        search.addActionListener(e->find.run());searchField.addActionListener(e->find.run());find.run();return page;
    }
    JPanel notificationsContent(){JPanel p=column();p.add(labelPad("Actividad de tu cuenta",FG,16));p.add(line());
        p.add(labelPad("♥  Da me gusta a una publicación para ver los cambios.",MUTED,13));
        for(Post post:posts)if(post.liked||post.reposted){String action=post.liked?"♥ Te gustó":"⟳ Reposteaste";p.add(labelPad(action+" una publicación de "+post.user,FG,12));p.add(line());}return p;}
    JPanel profileContent(){JPanel p=column();JPanel cover=new JPanel();cover.setBackground(new Color(42,51,59));cover.setPreferredSize(new Dimension(390,115));cover.setMaximumSize(new Dimension(Integer.MAX_VALUE,115));p.add(cover);
        p.add(labelPad("Tú",FG,18));p.add(labelPad("@usuario · Perfil local de prueba",MUTED,12));p.add(line());
        int count=0;for(Post post:posts)if(post.handle.equals("@usuario")){p.add(feedPost(post));p.add(line());count++;}
        if(count==0)p.add(labelPad("Aún no has publicado. Usa el botón + Publicar.",MUTED,13));
        JButton add=new RoundedButton("+ Crear publicación",BLUE,Color.WHITE,22);add.addActionListener(e->compose());p.add(padded(add,10,13));return p;}
    JPanel bookmarkContent(){JPanel p=column();int n=0;for(Post post:posts)if(post.bookmarked){p.add(feedPost(post));p.add(line());n++;}if(n==0)p.add(labelPad("Aún no guardaste publicaciones.",MUTED,13));return p;}

    static JLabel label(String text,Color color,int style,int size) {JLabel l=new JLabel(text);l.setForeground(color);l.setFont(font(style,size));l.setAlignmentX(Component.LEFT_ALIGNMENT);return l;}
    static JPanel column(){JPanel p=new ScrollColumn();p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));p.setBackground(BG);p.setAlignmentX(Component.LEFT_ALIGNMENT);return p;}
    static class ScrollColumn extends JPanel implements Scrollable {public Dimension getPreferredScrollableViewportSize(){return getPreferredSize();}public int getScrollableUnitIncrement(Rectangle r,int o,int d){return 20;}public int getScrollableBlockIncrement(Rectangle r,int o,int d){return 180;}public boolean getScrollableTracksViewportWidth(){return true;}public boolean getScrollableTracksViewportHeight(){return false;}}
    static JComponent line(){JPanel p=new JPanel();p.setBackground(BORDER);p.setPreferredSize(new Dimension(390,1));p.setMaximumSize(new Dimension(Integer.MAX_VALUE,1));return p;}
    static String ellipsize(String s,int n){return s.length()>n?s.substring(0,n-1)+"…":s;}
    static String compact(int n){if(n>=1000000)return String.format(Locale.US,"%.1f M",n/1000000.0);if(n>=10000)return String.format(Locale.US,"%.1f k",n/1000.0);return String.valueOf(n);}
    static JTextArea bodyText(String text,int size,Color color){JTextArea a=new JTextArea(text);styleTextArea(a,size);a.setForeground(color);a.setEditable(false);a.setFocusable(false);a.setOpaque(false);a.setBorder(null);return a;}
    static void styleTextArea(JTextArea a,int size){a.setFont(font(Font.PLAIN,size));a.setForeground(FG);a.setCaretColor(FG);a.setLineWrap(true);a.setWrapStyleWord(true);a.setBackground(BG);}
    static JPanel padded(JComponent c,int top,int sides){JPanel p=new JPanel(new BorderLayout());p.setBackground(BG);p.setBorder(new EmptyBorder(top,sides,top,sides));p.add(c);return p;}
    static JComponent indent(JComponent inner,int left,int top){JPanel p=new JPanel(new BorderLayout());p.setBackground(BG);p.setBorder(new EmptyBorder(top,left,0,0));p.setPreferredSize(new Dimension(346,inner.getPreferredSize().height+top));p.setMaximumSize(new Dimension(346,inner.getPreferredSize().height+top));p.add(inner,BorderLayout.CENTER);p.setAlignmentX(Component.LEFT_ALIGNMENT);return p;}
    static JLabel labelPad(String s,Color color,int size){JLabel l=label(s,color,Font.PLAIN,size);l.setBorder(new EmptyBorder(13,14,13,10));return l;}
    JButton multilineButton(String text,Color color,int size,Runnable run){
        JButton b=new JButton("<html><body style='width:278px'>"+escape(text)+"</body></html>");clearButton(b);b.setForeground(color);b.setFont(font(Font.PLAIN,size));b.setHorizontalAlignment(SwingConstants.LEFT);b.setAlignmentX(Component.LEFT_ALIGNMENT);b.addActionListener(e->run.run());return b;
    }
    static String escape(String s){return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\n","<br>");}
    static void clearButton(AbstractButton b){b.setOpaque(false);b.setContentAreaFilled(false);b.setBorderPainted(false);b.setFocusPainted(false);b.setBorder(new EmptyBorder(5,3,5,3));b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));}
    static JButton button(String text,int size,Color color){JButton b=new JButton(text);b.setFont(font(Font.PLAIN,size));b.setForeground(color);clearButton(b);return b;}
    static JButton iconButton(String glyph,Color c,Runnable onClick){JButton b=new JButton(new Glyph(glyph,c,21));clearButton(b);b.addActionListener(e->onClick.run());return b;}
    static JButton avatarButton(String initial,Color color,int size){JButton b=new JButton(initial){protected void paintComponent(Graphics g){Graphics2D gg=(Graphics2D)g.create();gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);gg.setColor(color);gg.fillOval(0,0,getWidth()-1,getHeight()-1);gg.setColor(Color.WHITE);gg.setFont(font(Font.BOLD,size/2));FontMetrics fm=gg.getFontMetrics();gg.drawString(initial,(getWidth()-fm.stringWidth(initial))/2,(getHeight()+fm.getAscent()-fm.getDescent())/2);gg.dispose();}};
        b.setPreferredSize(new Dimension(size,size));b.setMinimumSize(new Dimension(size,size));b.setMaximumSize(new Dimension(size,size));clearButton(b);return b;}
    JScrollPane scroller(JComponent component){
        JScrollPane pane=new JScrollPane(component,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        pane.getViewport().setBackground(BG);pane.setBorder(null);pane.getVerticalScrollBar().setUnitIncrement(19);
        pane.getVerticalScrollBar().setPreferredSize(new Dimension(5,0));return pane;
    }
    void alert(String s){JOptionPane.showMessageDialog(window,s,"Simulador X",JOptionPane.INFORMATION_MESSAGE);}

    static class RoundedButton extends JButton {
        Color bg,fg;int arc;
        RoundedButton(String text,Color bg,Color fg,int arc){super(text);this.bg=bg;this.fg=fg;this.arc=arc;setFont(font(Font.BOLD,12));setForeground(fg);setContentAreaFilled(false);setBorderPainted(false);setFocusPainted(false);setBorder(new EmptyBorder(10,13,10,13));setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));}
        protected void paintComponent(Graphics g){Graphics2D gg=(Graphics2D)g.create();gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);gg.setColor(bg);gg.fillRoundRect(0,0,getWidth(),getHeight(),arc,arc);gg.dispose();super.paintComponent(g);}
    }
    static class MediaPanel extends JPanel {
        MediaPanel(){setOpaque(false);setBorder(new LineBorder(BORDER,1,true));setAlignmentX(Component.LEFT_ALIGNMENT);}
        protected void paintComponent(Graphics gr){super.paintComponent(gr);Graphics2D g=(Graphics2D)gr.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int w=getWidth(),h=getHeight();g.setColor(new Color(250,250,250));g.fillRoundRect(1,1,w-2,h-2,15,15);
            int d=Math.min(w-55,h-40);int x=(w-d)/2,y=(h-d)/2;
            g.setColor(new Color(225,230,232));g.fillOval(x,y,d,d);g.setColor(Color.WHITE);g.fillOval(x+d/2-d/7,y+d/4-d/7,d/7*2,d/7*2);
            g.fillOval(x+d/5,y+d/2,d*3/5,d*3/5);g.dispose();}
    }
    static class Glyph implements Icon {
        String type;Color color;int sz;
        Glyph(String t,Color c,int s){type=t;color=c;sz=s;}
        public int getIconWidth(){return sz+4;}public int getIconHeight(){return sz+4;}
        public void paintIcon(Component c,Graphics graphics,int xx,int yy){Graphics2D g=(Graphics2D)graphics.create();g.translate(xx+2,yy+2);g.setColor(color);g.setStroke(new BasicStroke(1.65f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            double z=sz/20.0;g.scale(z,z);
            switch(type){
                case "back":g.drawLine(16,10,3,10);g.drawLine(3,10,10,3);g.drawLine(3,10,10,17);break;
                case "more":g.fillOval(2,9,3,3);g.fillOval(9,9,3,3);g.fillOval(16,9,3,3);break;
                case "home":{Path2D p=new Path2D.Double();p.moveTo(2,9);p.lineTo(10,2);p.lineTo(18,9);p.lineTo(18,18);p.lineTo(12,18);p.lineTo(12,12);p.lineTo(8,12);p.lineTo(8,18);p.lineTo(2,18);p.closePath();g.draw(p);break;}
                case "search":g.drawOval(2,2,12,12);g.drawLine(13,13,19,19);break;
                case "bell":{Path2D p=new Path2D.Double();p.moveTo(3,15);p.curveTo(5,13,5,12,5,9);p.curveTo(5,1,15,1,15,9);p.curveTo(15,12,15,13,17,15);p.closePath();g.draw(p);g.drawArc(8,15,4,4,180,180);break;}
                case "profile":g.drawOval(6,2,8,8);g.drawArc(2,10,16,13,0,180);break;
                case "reply":{Path2D p=new Path2D.Double();p.moveTo(4,4);p.curveTo(-1,9,2,16,8,16);p.lineTo(8,19);p.lineTo(12,15);p.curveTo(23,13,21,1,11,2);p.closePath();g.draw(p);break;}
                case "repost":g.drawLine(3,5,16,5);g.drawLine(16,5,13,2);g.drawLine(16,5,13,8);g.drawLine(17,15,4,15);g.drawLine(4,15,7,12);g.drawLine(4,15,7,18);g.drawLine(3,5,3,10);g.drawLine(17,10,17,15);break;
                case "heart":{Path2D p=new Path2D.Double();p.moveTo(10,18);p.curveTo(7,15,1,11,1,7);p.curveTo(1,1,8,0,10,5);p.curveTo(12,0,19,1,19,7);p.curveTo(19,11,13,15,10,18);g.draw(p);break;}
                case "chart":g.drawLine(2,18,18,18);g.drawRect(4,11,3,7);g.drawRect(9,7,3,11);g.drawRect(14,3,3,15);break;
                case "bookmark":{Path2D p=new Path2D.Double();p.moveTo(4,2);p.lineTo(16,2);p.lineTo(16,19);p.lineTo(10,15);p.lineTo(4,19);p.closePath();g.draw(p);break;}
            }g.dispose();}
    }
}
