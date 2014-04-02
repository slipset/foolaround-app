(ns foolaround-app.gui
    (:import
     (javax.swing JFrame JButton JOptionPane JPanel BorderFactory JLabel BoxLayout SwingUtilities SwingConstants)
     (javax.swing.border LineBorder)
     (javax.sound.midi MidiSystem Sequence MidiEvent ShortMessage Track)
     (java.awt Canvas Graphics Color Toolkit BorderLayout Font )
     (java.lang Thread)
     (java.awt.event ActionListener MouseAdapter))
    (:gen-class))

(def button_props
  [{:color Color/BLUE
    :sound [106 48]}
   {:color Color/RED
    :sound [106 52]}    
   {:color Color/GREEN
    :sound [106 55]}    
   {:color Color/YELLOW
    :sound [85 40]}])
   

(defn setup-gui [play_sound light_buttons update_queue]
  (let [player (MidiSystem/getSequencer)
        seq (Sequence. Sequence/PPQ 10)
        track (.createTrack seq)

        frame  (JFrame. "testFrame")
        panel (JPanel.)
        msg_display (JLabel. "Starting Game" SwingConstants/RIGHT)
                                        ;made the buttons JPanels vs. JButtons because I don't want JButton UI behavior
        buttonOne (JPanel. )
        buttonTwo (JPanel. )
        buttonThree (JPanel. )
                                        ; put them in a list, which gets passed around
        buttonlist [buttonOne buttonTwo buttonThree]
                                        ;make just ONE button listener for *all* buttons
                                        ; it gets the source of the event (which button) then uses the button(panel's) name to determine *which* button was pushed
                                        ; I set the button names later in this function
        button_listener 
        (proxy [MouseAdapter] []
          (mousePressed [evt]
            (let [buttonJustPushed (.getSource evt)
                  buttonNum (Integer/parseInt (.getName buttonJustPushed))
                  props (button_props (- buttonNum 1))]
            (play_sound (:sound props ) player track)
            (light_buttons buttonJustPushed (:color props))
            (update_queue buttonNum))))] 
    (.setSequence player seq)
    (.open player)
    (.addMouseListener buttonOne button_listener)
    (.addMouseListener buttonTwo button_listener)
    (.addMouseListener buttonThree button_listener)

    (.setName buttonOne "1")
    (.setName buttonTwo "2")
    (.setName buttonThree "3")

    (.setBackground buttonOne Color/darkGray)
    (.setBackground buttonTwo Color/darkGray)
    (.setBackground buttonThree Color/darkGray)

    (def myBorder (BorderFactory/createLineBorder Color/WHITE 12)) ;; USE Let

    (.setBorder buttonOne myBorder)
    (.setBorder buttonTwo myBorder)
    (.setBorder buttonThree myBorder)

    (.setSize buttonOne 200 200)
    (.setSize buttonTwo 200 200)
    (.setSize buttonThree 200 200)

    (def contentPane (.getContentPane frame)) ;; USE Let
    (.setFont msg_display (Font. Font/SANS_SERIF Font/BOLD 18))
    (.setBorder msg_display myBorder)

    (doto contentPane
      (.add BorderLayout/CENTER panel)
      (.add BorderLayout/NORTH msg_display))

    (doto panel
      (.add buttonOne)
      (.add buttonTwo)
      (.add buttonThree)
      (.setSize 420 300)
      (.setBackground Color/GRAY)
      (.setLayout (BoxLayout. panel BoxLayout/Y_AXIS)))
    
    (doto frame
      (.setSize 550 500)
      (.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE)
      (.setResizable false)
      (.setVisible true))
   [ buttonlist msg_display player track]))
