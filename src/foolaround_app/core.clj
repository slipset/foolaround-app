(ns foolaround-app.core
  (:import
   (javax.swing JFrame JButton JOptionPane JPanel BorderFactory JLabel BoxLayout SwingUtilities SwingConstants)
   (javax.swing.border LineBorder)
   (javax.sound.midi MidiSystem Sequence MidiEvent ShortMessage Track)
   (java.awt Canvas Graphics Color Toolkit BorderLayout Font )
   (java.lang Thread)
   (java.awt.event ActionListener MouseAdapter))
  (:gen-class))

(def button_prop_COLOR
  "vector of immutable  properties for sound and color of the buttons
   used by the 2 functions that play sound"
  [Color/BLUE Color/RED Color/GREEN Color/YELLOW Color/MAGENTA])
(def button_prop_noteAndInst [[106 48][106 52][106 55][85 40]])

(def button_queue
  "holds the button click events (as integers 1-3)"
  (atom [])) 

(defn update_queue [numToAdd]
  (if (> numToAdd 0)
    (swap! button_queue conj numToAdd)
    (reset! button_queue [])))

(defn check_guess
  "as soon as a sequence plays, this starts looping (recursive)
   looking at the button click events, comparing to current actual game sequence"
  [currSeq]
;; make a copy/deref the button queue
;; shorten it in case it's longer than the current Sequence 
;; then see if the button queue matches the current sequence
  (let [copyOfButtonQueue (deref button_queue)
        seqCount (count currSeq)
        shorterButtonQueue (take seqCount copyOfButtonQueue)
        buttonQueueCount (count (vec shorterButtonQueue))
        userRight? (= shorterButtonQueue (subvec currSeq 0 buttonQueueCount))
        ]
; make the decision to either keep checking, start next longer sequence, or start over
    (cond
     (and userRight? (= buttonQueueCount seqCount)) (+ 2 2) ; OMG AWFUL - this forces return of true
                                                            ; user did full sequence, so ready for next note 
     (and (not userRight?) (> buttonQueueCount 0)) (println "user wrong") ; user hit a wrong button, which will start new game
     :else  (recur currSeq)))))  ; keep checking, user not done w/ sequence but right so far so recall check_guess
                                        

(defn play_sound
  "this function take a vector w/ MIDI note & instrument integers, the MIDI sequencer, and track
   it sets the instrument and plays the note
   the MIDI sequencer and track were created in main method"
  [note_and_inst_vec player track]
  (.stop player) 
  (let [ currTickPosition (.getTickPosition player)
        noteToPlay (note_and_inst_vec 1)
        instrumentToPlay (note_and_inst_vec 0)
        instMessage (ShortMessage.)
        changeInstEvent (MidiEvent. instMessage currTickPosition) 
        noteonmessage (ShortMessage.)
        noteon (MidiEvent. noteonmessage currTickPosition)
        noteoffmessage (ShortMessage.)
        noteoff (MidiEvent. noteoffmessage (+ currTickPosition 12))]
    (.setMessage instMessage 192 1 instrumentToPlay 0)
    (.add track changeInstEvent) 
    (.setMessage noteonmessage 144 1 noteToPlay 100) 
    (.add track noteon)
    (.setMessage noteoffmessage 128 1 noteToPlay 100) 
    (.add track noteoff)    
                                        ; now PLAY all messages we added to the track
    (.start player)
    (Thread/sleep 100)))  ; attempt to control the timing between notes

(defn light_buttons
  "just takes a button (which is actually just a 'panel', not UI button)
   and a color and changes the color, then changes back to original dark gray
   BAD: for now, I hard-coded the 'default' color state"
  [button color]
  (.setBackground button color)
  (.paintImmediately button 0 0
                     (.getWidth button)
                     (.getHeight button))
  (Thread/sleep 160)  ; this determines how long the button "lights"
  (.setBackground button Color/darkGray))

(defn main_game_loop
   "most of the game is here -- it is recursive, it basically:
    play a sequence of notes then call check guess which keeps checking the user's event queue 
    based on check guess return, either add 1 note to sequence OR start over"
   [buttonlist msg_display currentSequence player track]
  (Thread/sleep 500) ; aack everything fails if I don't do this pause here!
                                        ; calls the atom / changes the state---------------------------
  (update_queue 0) ; zero's out the list of action events from the user
                   ; because each time we play a sequence, user has to start clicking again from the beginning
                  ; this is awkward, but I'm too lazy to create a second JLabel to display the score
  (.setText msg_display (str "Playing new sequence. Score: "  (count currentSequence))) 

                                        ; add 1 to the current sequence of notes
  ;; Don't do this. EVER
  (def newSequence (conj currentSequence (+ (rand-int 3) 1)))
                                        ; now PLAY the sequence
  (doseq [sequencePart newSequence]  ; this is just an int for which button in buttonlist
    (do (def buttonNum (- sequencePart 1)) ; this gives us the index into buttonlist 
                                        ; AND button property lists

        (def buttonToPlay (buttonlist buttonNum))  ; figure out which ACTUAL button to play
        (play_sound (button_prop_noteAndInst buttonNum) player track)
        (light_buttons buttonToPlay (button_prop_COLOR buttonNum))))
  
  (.setText msg_display (str "Now Your Turn. Score: "  (count currentSequence)))

                                        ; now start the checking user guess 'loop'
  (if (nil? (check_guess newSequence))
    (do                                     ; user made a wrong move play an ugly sound, then start over
      (play_sound (button_prop_noteAndInst 3) player track)
      (.setText msg_display (str "SORRY. Final Score: "  (count currentSequence)))  ; must start over
      (Thread/sleep 2000)
      (.setText msg_display "Starting Over")
                                        ; (main_game_loop buttonlist msg_display [] player track) ; diff send empty sequence
      (recur buttonlist msg_display [] player track))
                                        ; ELSE condition -- they answered correctly, recursively call this function again
    (recur buttonlist msg_display newSequence player track)))


(defn startgame
   "this is called only the very first time, NOT when user fails & new game starts again"
  [buttonlist msg_display player track]
  (.setText msg_display "Starting New Game")
  (Thread/sleep 2000)
  (.setText msg_display "Watch and listen!")
  (main_game_loop buttonlist msg_display [] player track))

(defn -main  [& args]
  (println "in main")
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
            (def buttonJustPushed (.getSource evt)) ; DONT
            
            (def buttonNum (Integer/parseInt (.getName buttonJustPushed))) ; DONT
            (play_sound (button_prop_noteAndInst (- buttonNum 1)) player track)
            (light_buttons buttonJustPushed (button_prop_COLOR (- buttonNum 1)))
            (update_queue buttonNum)))] 
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
    (startgame buttonlist msg_display player track)))

