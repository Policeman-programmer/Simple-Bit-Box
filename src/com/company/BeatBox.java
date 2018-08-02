package com.company;

import javax.sound.midi.*;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

public class BeatBox {
    JFrame theFrame;
    JPanel mainPanel;
    JList incomingList;
    JTextArea myMassage;
    ArrayList<JCheckBox> chekBoxList;
    int nextNum;
    Vector<String> listVector = new Vector<>();
    String userName = "";
    ObjectOutputStream out; //это будет отправлять сообщения и сериализованый обьект
    ObjectInputStream in;//а это будет принимать
    HashMap<String, boolean[]> otherSeqsMap = new HashMap<String, boolean[]>();

    Sequencer sequencer;
    Sequence sequence;
    Sequence mySequance = null;
    Track track;

    String[] instrumentNames = {"Bass Drum", "Closed Hi-Hat", "Open Hi-Hat", "Acoustic Snare", "Crash Cymbal",
            "Hand Clap", "Hight Tom", "Hi Bongo", "Maracas", "Whistle", "Low Conga", "Cowbell", "Vibraslap", "Low -mid Tom",
            "High Agogo", "Open Hi Conga"};

    int[] instruments = {35, 42, 46, 38, 49, 39, 50, 60, 70, 72, 64, 56, 58, 47, 67, 63};


    public static void main(String[] args) {
        new BeatBox().startUp();
    }

    private void startUp() {
        //открываем соединение с сервером
        try {
            Socket socket = new Socket("127.0.0.1", 4242);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            Thread remote = new Thread(new RemoteReader());
            remote.start();
        } catch (IOException e) {
            System.out.println("не смог подключится, ты будешь играть один!");
        }
        setUpMidi();
        buildGUI();
    }

    public void buildGUI() {
        theFrame = new JFrame("Cyber BeatBox");
        theFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        // BorderLayout layout = new BorderLayout();
        JPanel background = new JPanel(new BorderLayout());
        background.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        chekBoxList = new ArrayList<>();
        Box bottonBox = new Box(BoxLayout.Y_AXIS); //АНАЛОГИЧНО что Box.createVerticalBox;

        JButton start = new JButton("Start");
        start.addActionListener(new MyStartListener());
        bottonBox.add(start);

        JButton stop = new JButton("Stop");
        stop.addActionListener(new MyStopListener());
        bottonBox.add(stop);

        JButton upTempo = new JButton("Tempo Up");
        upTempo.addActionListener(new MyUpTempoListener());
        bottonBox.add(upTempo);

        JButton downTempo = new JButton("Tempo Down");
        downTempo.addActionListener(new MyDownTempoListener());
        bottonBox.add(downTempo);

        JButton serialize = new JButton("SerializeIt");
        serialize.addActionListener(new MySaveListenr());
        bottonBox.add(serialize);

        JButton restore = new JButton("restore");
        restore.addActionListener(new MyReaderInListener());
        bottonBox.add(restore);

        JButton cleanAll = new JButton("Clean All");
        cleanAll.addActionListener(new CleanAll());
        bottonBox.add(cleanAll);

        JButton send = new JButton("send it");
        send.addActionListener(new SendIt());
        bottonBox.add(send);

        JButton randomSequance = new JButton("random sequanse");
        randomSequance.addActionListener(new RandomSequance());
        bottonBox.add(randomSequance);

        myMassage = new JTextArea(3, 20);
        myMassage.setLineWrap(true);
        myMassage.setWrapStyleWord(true);
        JScrollPane scroller = new JScrollPane(myMassage);
        scroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        bottonBox.add(scroller);

        incomingList = new JList();
        incomingList.addListSelectionListener(new MyListSelectionListener());
        incomingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane theList = new JScrollPane(incomingList);
        bottonBox.add(theList);
        incomingList.setListData(listVector);

        Box nameBox = new Box(BoxLayout.Y_AXIS);
        for (int i = 0; i < instrumentNames.length; i++) {
            nameBox.add(new JLabel(instrumentNames[i]));
            nameBox.add(Box.createVerticalStrut(8));
        }

        background.add(BorderLayout.EAST, bottonBox);
        background.add(BorderLayout.WEST, nameBox);

        theFrame.getContentPane().add(background);

        //GridLayout grid = new GridLayout(16,16,2,1);
        mainPanel = new JPanel(new GridLayout(16, 16, 2, 1));
        background.add(BorderLayout.CENTER, mainPanel);

        for (int i = 0; i < 256; i++) {
            JCheckBox c = new JCheckBox();
            c.setSelected(false);
            chekBoxList.add(c);
            mainPanel.add(c);
        }
        theFrame.setBounds(50, 50, 300, 300);
        theFrame.pack();
        theFrame.setVisible(true);

        while (userName.equals("")) {
            userName = (String) JOptionPane.showInputDialog(theFrame, "Как тебя зовут?","проверка имени",
                    JOptionPane.PLAIN_MESSAGE, null, null,"");
        }
    }

    public void setUpMidi() {
        try {
            sequencer = MidiSystem.getSequencer();//создаем проигрыватель сиквенсор
            sequencer.open();//открываем эго
            sequence = new Sequence(Sequence.PPQ, 4);//создаем последовательность
            track = sequence.createTrack();//создаем трек
            sequencer.setTempoInBPM(120);//устанавливаем ему темп
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void buildTrackAndStart() {
        ArrayList<Integer> trackList = null;

        sequence.deleteTrack(track);//удаляем старый трек
        track = sequence.createTrack();//добавляем последовательность в трек

        for (int i = 0; i < 16; i++) {
            trackList = new ArrayList<Integer>();
            for (int j = 0; j < 16; j++) {
                JCheckBox jc = (JCheckBox) chekBoxList.get(j + (16 * i));
                if (jc.isSelected()) {
                    int key = instruments[i];
                    trackList.add(new Integer(key));
                } else {
                    trackList.add(null);
                }
            }
            makeTrack(trackList);
            track.add(makeEvent(192, 9, 1, 0, 15));
        }
        //track.add(makeEvent(192,9,1,0,15));
        try {
            sequencer.setSequence(sequence);
            sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);
            sequencer.start();
            sequencer.setTempoInBPM(120);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void makeTrack(ArrayList<Integer> trackList) {
        Iterator it = trackList.iterator();
        for (int i = 0; i < 16; i++) {
            Integer num = (Integer) it.next();
            if(num!=null){
                int key = num.intValue();
                track.add(makeEvent(144, 9, key, 100, i));
                track.add(makeEvent(128, 9, key, 100, i + 1));
            }
        }
    }

    public class MyStartListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            buildTrackAndStart();
            System.out.println("go");
        }
    }

    public class MyStopListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            sequencer.stop();
            System.out.println("stop");
        }
    }

    public class MyUpTempoListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            float tempoFactor = sequencer.getTempoFactor();
            sequencer.setTempoFactor((float) (tempoFactor * 1.03));
            System.out.println("tempo up");
        }
    }

    public class MyDownTempoListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            float tempoFactor = sequencer.getTempoFactor();
            sequencer.setTempoFactor((float) (tempoFactor * 0.97));
            System.out.println("tempo down");
        }
    }

    public class MySaveListenr implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            System.out.println("Save");
            boolean[] checkBoxState = new boolean[256];
            for (int i = 0; i < 256; i++) {
                JCheckBox check = chekBoxList.get(i);
                if (check.isSelected()) {
                    checkBoxState[i] = true;
                }
            }

            try {
                JFileChooser chooser = new JFileChooser();
                chooser.showSaveDialog(theFrame);
                FileOutputStream fileStrem = new FileOutputStream(chooser.getSelectedFile());
                ObjectOutputStream os = new ObjectOutputStream(fileStrem);
                os.writeObject(checkBoxState);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public class MyReaderInListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            System.out.println("restore");
            boolean[] checkBoxState = null;
            try {
                JFileChooser chooser = new JFileChooser();
                chooser.showOpenDialog(theFrame);
                FileInputStream imputStrem = new FileInputStream(chooser.getSelectedFile());
                ObjectInputStream is = new ObjectInputStream(imputStrem);
                checkBoxState = (boolean[]) is.readObject();

            } catch (Exception ex) {
                ex.printStackTrace();
            }
            for (int i = 0; i < 256; i++) {
                JCheckBox check = (JCheckBox) chekBoxList.get(i);
                if (checkBoxState[i]) {
                    check.setSelected(true);
                } else {
                    check.setSelected(false);
                }
            }
            sequencer.stop();
            buildTrackAndStart();
        }
    }

    public class CleanAll implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            for (int i = 0; i < chekBoxList.size(); i++) {
                chekBoxList.get(i).setSelected(false);
            }
        }
    }

    private class SendIt implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            boolean[] checkboxState = new boolean[256];
            for (int i = 0; i < 256; i++) {
                if (chekBoxList.get(i).isSelected()) {
                    checkboxState[i] = true;
                }
            }
            try {
                out.writeObject(userName + nextNum++ + ": " + myMassage.getText());
                out.writeObject(checkboxState);
            } catch (IOException e1) {
                System.out.println("извини чувак, это не отсылается на сервак");
            }
        }
    }


    public class MyListSelectionListener implements ListSelectionListener {

        @Override
        public void valueChanged(ListSelectionEvent e) {
            if (!e.getValueIsAdjusting()) {//если событие вызвано не многострочным выбором листа
                String seleted = (String) incomingList.getSelectedValue();
                if (seleted != null) {
                    //переходим к отображению и изменяем последовательность
                    boolean[] selectedState = (boolean[]) otherSeqsMap.get(seleted);
                    changeSequance(selectedState);
                    sequencer.stop();
                    buildTrackAndStart();
                }
            }
        }
    }

    public class RemoteReader implements Runnable {
        boolean[] chackboxState = null;
        String nameToShow = null;
        Object obj = null;

        @Override

        public void run() {
            try {
                while ((obj = in.readObject()) != null) {
                    System.out.println("получили обьект из сервака");
                    System.out.println(obj.getClass());
                    String nameToShow = (String) obj;
                    chackboxState = (boolean[]) in.readObject();
                    otherSeqsMap.put(nameToShow, chackboxState);
                    listVector.add(nameToShow);
                    incomingList.setListData(listVector);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public class MyPlayMineLisrener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (mySequance != null) {
                sequence = mySequance;
            }
        }
    }

    private void changeSequance(boolean[] selectedState) {
        for (int i = 0; i < 256; i++) {
            if (selectedState[i]) {
                chekBoxList.get(i).setSelected(true);
            } else {
                chekBoxList.get(i).setSelected(false);
            }
        }
    }

    private MidiEvent makeEvent(int com, int chan, int melody, int silaNazima, int tick) {
        MidiEvent me = null;
        try {
            ShortMessage sm = new ShortMessage(com, chan, melody, silaNazima);
            me = new MidiEvent(sm, tick);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return me;
    }

    private class RandomSequance implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            int random;
            for (int i = 0; i <256 ; i++) {
                random = (int)(Math.random()*2);
                if(random == 1){
                    int nextrandom = (int)(Math.random()*2);
                    if(nextrandom ==1){
                        chekBoxList.get(i).setSelected(true);
                    }else {
                        chekBoxList.get(i).setSelected(false);
                    }

                }
                else {
                    chekBoxList.get(i).setSelected(false);
                }
            }
        }
    }
}

