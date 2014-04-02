(ns foolaround-app.core
  (:import
   (javax.swing JFrame JButton JOptionPane JPanel BorderFactory JLabel BoxLayout SwingUtilities SwingConstants)
   (javax.swing.border LineBorder)
   (javax.sound.midi MidiSystem Sequence MidiEvent ShortMessage Track)
   (java.awt Canvas Graphics Color Toolkit BorderLayout Font )
   (java.lang Thread)
   (java.awt.event ActionListener MouseAdapter))
  (:gen-class))

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
     :else  (recur currSeq))))  ; keep checking, user not done w/ sequence but right so far so recall check_guess
                                        

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
    (let [buttonNum (- sequencePart 1) ; this gives us the index into buttonlist 
          buttonToPlay (buttonlist buttonNum)  ; figure out which ACTUAL button to play
          props (foolaround-app.gui/button_props buttonNum)]
        (play_sound (:sound props) player track)
        (light_buttons buttonToPlay (:color props))))
  
  (.setText msg_display (str "Now Your Turn. Score: "  (count currentSequence)))

                                        ; now start the checking user guess 'loop'
  (if (nil? (check_guess newSequence))
    (do                                     ; user made a wrong move play an ugly sound, then start over
      (play_sound (foolaround-app.gui/button_prop_noteAndInst 3) player track)
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
      (apply startgame (foolaround-app.gui/setup-gui play_sound light_buttons update_queue)))

(-main)


