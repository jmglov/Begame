(ns ant-mine.quadtree
  (:refer-clojure :exclude [contains?]))

(defn contains?
  [{x1 :x y1 :y height1 :height width1 :width}
   {x2 :x y2 :y height2 :height width2 :width :or {width2 0 height2 0}}]
  (and (<= x1 x2)
       (<= y1 y2)
       (>= (+ x1 width1) (+ x2 width2))
       (>= (+ y1 height1) (+ y2 height2))))

(defn overlaps? [bb bb2]
  (let [overlaps*
          (fn [bb {:keys [x y width height]}]
            (let [y2 (+ y height)
                  x2 (+ x width)]
              (or (contains? bb {:x x :y y})
                  (contains? bb {:x x :y y2})
                  (contains? bb {:x x2 :y y})
                  (contains? bb {:x x2 :y y2}))))]
        (or (overlaps* bb2 bb)
            (overlaps* bb bb2))))

(defn has-centre? [bb {:keys [x y width height]}]
  (contains? bb {:x (+ x (/ width 2)), :y (+ y (/ height 2))}))

(defrecord quad [x y width height content])

(defn area
  ([width height]
   (area 0 0 width height))
  ([x y width height]
   (area x y width height [nil nil nil nil]))
  ([x y width height content]
   (quad. x y width height content)))

(defn sub-area [idx field]
  (let [width (/ (:width field) 2)
        height (/ (:height field) 2)]
    (let [[x y] (case idx
                      0 [0 0]
                      1 [width 0]
                      2 [0 height]
                      3 [width height])]
      (area (+ x (:x field)) (+ y (:y field)) width height))))

(defn paint-quad [obj g pane]
  (.setColor g (java.awt.Color. (rand-int 0xffffff)))
  (if (instance? quad obj)
    (do
      (.fillRect g (:x obj) (:y obj) (:width obj) (:height obj))
      (doseq [child (:content obj)
              :when (not (nil? child))]
        (paint-quad child g pane)))
    (.drawImage g (:sprite obj) (:x obj) (:y obj) pane)))

(defn insert [field obj & [pidx parent]]
  (cond
    (nil? field) obj
    (or (<= (:width field) 1)
        (<= (:height field) 1))
      (throw (Exception. "another object exists here"))
    (instance? quad field)
      (let [quads (map #(sub-area % field) (range 4))
            idx (first
                  (keep-indexed
                    #(when (has-centre? %2 obj) %1)
                    quads))]
        (update-in field [:content idx]
                   insert obj idx field))
    :else (-> (sub-area pidx parent)
            (insert field)
            (insert obj))))

(defn collides? [field obj]
  (cond
    (nil? field) false
    (instance? quad field)
      (reduce #(or %1 (collides? %2 obj))
        false
        (filter
          (partial overlaps? obj)
          (keep identity (:content field))))
    :else (when (overlaps? field obj) field)))
