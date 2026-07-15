package com.neet.tracker;

import java.util.LinkedHashMap;
import java.util.Map;

/** Static syllabus data, ported from SYLLABUS in neet-tracker.jsx */
public class Syllabus {

    public static class Subject {
        public String key, label, color;
        public LinkedHashMap<String, String[]> groups = new LinkedHashMap<>();
        Subject(String key, String label, String color) {
            this.key = key; this.label = label; this.color = color;
        }
    }

    public static final String[] SUBTASK_KEYS = {"pw", "aakash", "pyq", "notes", "mtg"};
    public static final String[] SUBTASK_LABELS = {"PW Module", "Aakash Module", "PYQ", "Notes", "MTG"};

    public static final LinkedHashMap<String, Subject> SUBJECTS = new LinkedHashMap<>();

    static {
        Subject physics = new Subject("physics", "Physics", "#0E7C7B");
        physics.groups.put("Class 11", new String[]{
            "Physical World & Measurement","Motion in a Straight Line","Motion in a Plane",
            "Laws of Motion","Work, Energy and Power","System of Particles & Rotational Motion",
            "Gravitation","Mechanical Properties of Solids","Mechanical Properties of Fluids",
            "Thermal Properties of Matter","Thermodynamics","Kinetic Theory","Oscillations","Waves"
        });
        physics.groups.put("Class 12", new String[]{
            "Electric Charges and Fields","Electrostatic Potential & Capacitance","Current Electricity",
            "Moving Charges and Magnetism","Magnetism and Matter","Electromagnetic Induction",
            "Alternating Current","Electromagnetic Waves","Ray Optics","Wave Optics",
            "Dual Nature of Radiation and Matter","Atoms","Nuclei","Semiconductor Electronics"
        });
        SUBJECTS.put("physics", physics);

        Subject chemistry = new Subject("chemistry", "Chemistry", "#2FAF7A");
        chemistry.groups.put("Class 11", new String[]{
            "Basic Concepts of Chemistry","Structure of Atom","Classification of Elements & Periodicity",
            "Chemical Bonding & Molecular Structure","States of Matter","Thermodynamics","Equilibrium",
            "Redox Reactions","Hydrogen","s-Block Elements","p-Block Elements (13 & 14)",
            "Organic Chemistry — Basic Principles","Hydrocarbons","Environmental Chemistry"
        });
        chemistry.groups.put("Class 12", new String[]{
            "Solid State","Solutions","Electrochemistry","Chemical Kinetics","Surface Chemistry",
            "Isolation of Elements","p-Block Elements (15–18)","d and f Block Elements",
            "Coordination Compounds","Haloalkanes and Haloarenes","Alcohols, Phenols and Ethers",
            "Aldehydes, Ketones and Carboxylic Acids","Amines","Biomolecules","Polymers",
            "Chemistry in Everyday Life"
        });
        SUBJECTS.put("chemistry", chemistry);

        Subject biology = new Subject("biology", "Biology", "#FF6A55");
        biology.groups.put("Class 11", new String[]{
            "Living World","Biological Classification","Plant Kingdom","Animal Kingdom",
            "Morphology of Flowering Plants","Anatomy of Flowering Plants",
            "Structural Organisation in Animals","Cell: The Unit of Life","Biomolecules",
            "Cell Cycle and Cell Division","Transport in Plants","Mineral Nutrition","Photosynthesis",
            "Respiration in Plants","Plant Growth and Development","Digestion and Absorption",
            "Breathing and Exchange of Gases","Body Fluids and Circulation","Excretory Products",
            "Locomotion and Movement","Neural Control and Coordination","Chemical Coordination and Integration"
        });
        biology.groups.put("Class 12", new String[]{
            "Reproduction in Organisms","Sexual Reproduction in Flowering Plants","Human Reproduction",
            "Reproductive Health","Principles of Inheritance and Variation","Molecular Basis of Inheritance",
            "Evolution","Human Health and Disease","Microbes in Human Welfare",
            "Biotechnology: Principles and Processes","Biotechnology and its Applications",
            "Organisms and Populations","Ecosystem","Biodiversity and Conservation"
        });
        SUBJECTS.put("biology", biology);
    }

    public static final String[] SUBJECT_KEYS = {"physics", "chemistry", "biology"};

    public static final Map<String, String> EXTRA_ACTIVITIES = new LinkedHashMap<>();
    static {
        EXTRA_ACTIVITIES.put("sleep", "Sleep");
        EXTRA_ACTIVITIES.put("chores", "Personal Chores");
    }

    public static final String NEET_EXAM_DATE = "2027-05-01";
}
