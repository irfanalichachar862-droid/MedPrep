package com.example.data

object CsvMcqParser {

    val TEMPLATE_CSV = """
subject,topic,question,option_a,option_b,option_c,option_d,correct,reason,youtube_url,difficulty,status
Biology,Cell Biology,"Which organelle is primarily responsible for ATP synthesis via oxidative phosphorylation?",Golgi apparatus,Mitochondria,Lysosome,Smooth endoplasmic reticulum,B,"Mitochondria generate most cellular ATP through the electron transport chain and chemiosmosis on the inner mitochondrial membrane.",https://www.youtube.com/results?search_query=Biology+Cell+Biology+Mitochondria+ATP+MDCAT,easy,published
Chemistry,Organic Chemistry,"Which functional group characterizes aldehydes?",-COOH,-CHO,-OH,-NH2,B,"Aldehydes contain a terminal carbonyl group bonded to at least one hydrogen atom (-CHO).",https://www.youtube.com/results?search_query=Chemistry+Aldehydes+Functional+Group+MDCAT,easy,published
Physics,Mechanics,"If the net force acting on a 4 kg object is 20 N, what is its acceleration?",2 m/s^2,4 m/s^2,5 m/s^2,80 m/s^2,C,"By Newton's second law, a = F / m = 20 N / 4 kg = 5 m/s^2.",https://www.youtube.com/results?search_query=Physics+Newtons+Second+Law+MDCAT,easy,published
English,Grammar,"Choose the sentence with correct subject-verb agreement.","Neither the students nor the teacher were ready.","Neither the students nor the teacher was ready.","Neither the student nor the teachers was ready.","Neither the teacher nor the students is ready.",B,"With 'neither...nor', the verb agrees with the nearer subject ('the teacher' is singular, so 'was' is correct).",https://www.youtube.com/results?search_query=English+Subject+Verb+Agreement+Neither+Nor+MDCAT,medium,published
Logical Reasoning,Critical Thinking,"All doctors are graduates. Some graduates are researchers. Which conclusion definitely follows?","All doctors are researchers.","Some researchers are doctors.","Some graduates are doctors.","No doctor is a researcher.",C,"Since all doctors belong to the set of graduates, it logically follows that some graduates are doctors.",https://www.youtube.com/results?search_query=Logical+Reasoning+Syllogism+MDCAT,medium,published
""".trimIndent()

    fun parseCsv(rawText: String, defaultStatus: String = "published"): CsvParseResult {
        val cleaned = rawText.removePrefix("\uFEFF").trim()
        if (cleaned.isBlank()) {
            return CsvParseResult(emptyList(), listOf("File or text is empty."))
        }

        val delimiter = detectDelimiter(cleaned)
        val rows = parseDelimitedRows(cleaned, delimiter).filter { row ->
            row.any { it.isNotBlank() }
        }
        return parseRows(rows, defaultStatus)
    }

    fun parseRows(rows: List<List<String>>, defaultStatus: String = "published"): CsvParseResult {
        if (rows.isEmpty()) {
            return CsvParseResult(emptyList(), listOf("No rows found to import."))
        }

        val firstRowLower = rows.first().map { it.trim().lowercase() }
        val hasHeader = firstRowLower.any {
            it in setOf("question", "stem", "option_a", "option a", "correct", "answer", "subject", "reason")
        }

        val headerMap = if (hasHeader) {
            firstRowLower.withIndex().associate { (idx, name) -> normalizeHeader(name) to idx }
        } else {
            emptyMap()
        }

        val dataRows = if (hasHeader) rows.drop(1) else rows
        val valid = mutableListOf<QuestionDraftInput>()
        val errors = mutableListOf<String>()

        dataRows.forEachIndexed { index, cols ->
            val rowNum = if (hasHeader) index + 2 else index + 1
            fun col(key: String, fallbackIndex: Int): String {
                val idx = headerMap[key] ?: fallbackIndex
                return cols.getOrNull(idx)?.trim().orEmpty()
            }
            fun colExact(key: String, fallbackIndex: Int, maxLen: Int): String {
                val idx = headerMap[key] ?: fallbackIndex
                return FormulaTextPreserver.preserveExactStructure(cols.getOrNull(idx).orEmpty(), maxLen)
            }

            val rawSubject = col("subject", 0)
            val subject = canonicalSubject(rawSubject)
            val topic = col("topic", 1).take(120)
            val stem = colExact("stem", 2, 2000)
            val optionA = colExact("option_a", 3, 1000)
            val optionB = colExact("option_b", 4, 1000)
            val optionC = colExact("option_c", 5, 1000)
            val optionD = colExact("option_d", 6, 1000)
            val rawCorrect = col("correct", 7).uppercase().trim()
            val correct = when {
                rawCorrect in setOf("A", "B", "C", "D") -> rawCorrect
                rawCorrect.startsWith("OPTION ") && rawCorrect.removePrefix("OPTION ").trim() in setOf("A", "B", "C", "D") ->
                    rawCorrect.removePrefix("OPTION ").trim()
                rawCorrect == "1" -> "A"
                rawCorrect == "2" -> "B"
                rawCorrect == "3" -> "C"
                rawCorrect == "4" -> "D"
                else -> ""
            }
            val reason = colExact("explanation", 8, 3000)

            // Support both 11-column sheets (legacy without youtube_url) and 12-column sheets (with youtube_url)
            val hasYoutubeHeader = headerMap.containsKey("youtube_url")
            val rawYoutube = if (hasYoutubeHeader) {
                col("youtube_url", 9)
            } else {
                val candidate = cols.getOrNull(9)?.trim().orEmpty()
                if (candidate.startsWith("http://", ignoreCase = true) || candidate.startsWith("https://", ignoreCase = true)) {
                    candidate
                } else ""
            }
            val youtubeUrl = sanitizeOptionalHttpUrl(rawYoutube)

            val difficultyFallback = if (hasYoutubeHeader || rawYoutube.isNotBlank()) 10 else 9
            val statusFallback = if (hasYoutubeHeader || rawYoutube.isNotBlank()) 11 else 10

            val rawDifficulty = col("difficulty", difficultyFallback).lowercase()
            val difficulty = if (rawDifficulty in setOf("easy", "medium", "hard")) rawDifficulty else "medium"
            val rawStatus = col("status", statusFallback).lowercase()
            val status = if (rawStatus in setOf("draft", "published")) rawStatus else defaultStatus

            when {
                !MedPrepConstants.isCoreSubject(subject) ->
                    errors.add("Row $rowNum: Subject '$rawSubject' is not one of the 5 allowed subjects (Biology, Chemistry, Physics, English, Logical Reasoning).")
                stem.length < 5 -> errors.add("Row $rowNum: Question stem must be at least 5 characters.")
                optionA.isBlank() || optionB.isBlank() || optionC.isBlank() || optionD.isBlank() ->
                    errors.add("Row $rowNum: All four options (A, B, C, D) are required.")
                correct.isBlank() ->
                    errors.add("Row $rowNum: Correct column must be A, B, C, or D (got '${col("correct", 7)}').")
                else -> {
                    valid.add(
                        QuestionDraftInput(
                            subject = subject,
                            topic = topic,
                            stem = stem,
                            optionA = optionA,
                            optionB = optionB,
                            optionC = optionC,
                            optionD = optionD,
                            correctOption = correct,
                            explanation = reason,
                            youtubeUrl = youtubeUrl,
                            difficulty = difficulty,
                            status = status
                        )
                    )
                }
            }
        }

        return CsvParseResult(valid, errors)
    }

    fun sanitizeOptionalHttpUrl(raw: String?): String? {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isBlank()) return null
        val normalized = when {
            trimmed.startsWith("https://", ignoreCase = true) ||
                trimmed.startsWith("http://", ignoreCase = true) -> trimmed
            trimmed.startsWith("www.", ignoreCase = true) ||
                trimmed.startsWith("youtube.com", ignoreCase = true) ||
                trimmed.startsWith("youtu.be", ignoreCase = true) -> "https://$trimmed"
            else -> return null
        }
        return normalized.take(500)
    }

    fun canonicalSubject(raw: String): String {
        val trimmed = raw.trim()
        return when (trimmed.lowercase()) {
            "bio", "biology" -> "Biology"
            "chem", "chemistry" -> "Chemistry"
            "phy", "physic", "physics" -> "Physics"
            "eng", "english" -> "English"
            "lr", "logic", "logical", "logical reasoning", "reasoning" -> "Logical Reasoning"
            "" -> "Biology"
            else -> trimmed.take(80)
        }
    }

    private fun normalizeHeader(name: String): String = when (name.replace(" ", "_").replace("-", "_")) {
        "subject", "category", "discipline" -> "subject"
        "topic", "subtopic", "system", "chapter" -> "topic"
        "question", "stem", "prompt", "mcq", "question_text" -> "stem"
        "option_a", "a", "choice_a", "opt_a" -> "option_a"
        "option_b", "b", "choice_b", "opt_b" -> "option_b"
        "option_c", "c", "choice_c", "opt_c" -> "option_c"
        "option_d", "d", "choice_d", "opt_d" -> "option_d"
        "correct", "correct_option", "answer", "correct_answer", "key" -> "correct"
        "reason", "why", "correct_reason", "answer_reason", "explanation", "rationale", "solution", "notes" -> "explanation"
        "youtube_url", "youtube", "youtube_link", "video", "video_url", "video_link" -> "youtube_url"
        "difficulty", "level" -> "difficulty"
        "status", "state" -> "status"
        else -> name
    }

    private fun detectDelimiter(text: String): Char {
        val firstLine = text.lineSequence().firstOrNull().orEmpty()
        val tabs = firstLine.count { it == '\t' }
        val commas = firstLine.count { it == ',' }
        val semicolons = firstLine.count { it == ';' }
        return when {
            tabs > commas && tabs >= semicolons -> '\t'
            semicolons > commas -> ';'
            else -> ','
        }
    }

    private fun parseDelimitedRows(input: String, delimiter: Char): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < input.length) {
            val c = input[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < input.length && input[i + 1] == '"') {
                        field.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    field.append(c)
                }
            } else {
                when (c) {
                    '"' -> inQuotes = true
                    delimiter -> {
                        currentRow.add(field.toString())
                        field.setLength(0)
                    }
                    '\r' -> {
                        if (i + 1 < input.length && input[i + 1] == '\n') i++
                        currentRow.add(field.toString())
                        field.setLength(0)
                        rows.add(currentRow.toList())
                        currentRow.clear()
                    }
                    '\n' -> {
                        currentRow.add(field.toString())
                        field.setLength(0)
                        rows.add(currentRow.toList())
                        currentRow.clear()
                    }
                    else -> field.append(c)
                }
            }
            i++
        }

        if (field.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(field.toString())
            rows.add(currentRow.toList())
        }
        return rows
    }

    fun buildHighYieldStarterPack(): List<QuestionDraftInput> = listOf(
        // --- BIOLOGY (3) ---
        QuestionDraftInput(
            subject = "Biology",
            topic = "Cell Structure & Function",
            stem = "Which eukaryotic organelle contains its own circular DNA, 70S ribosomes, and cristae folded to maximize ATP synthesis?",
            optionA = "Golgi apparatus",
            optionB = "Mitochondrion",
            optionC = "Rough endoplasmic reticulum",
            optionD = "Peroxisome",
            correctOption = "B",
            explanation = "Mitochondria are semi-autonomous organelles with double membranes, circular DNA, and 70S ribosomes; inner membrane cristae house the electron transport chain for ATP production.",
            youtubeUrl = "https://www.youtube.com/results?search_query=Mitochondria+Structure+and+Function+MDCAT+Biology",
            difficulty = "easy",
            status = "published"
        ),
        QuestionDraftInput(
            subject = "Biology",
            topic = "Human Physiology",
            stem = "In the human nephron, where is the highest percentage of filtered glucose and amino acids reabsorbed into the blood?",
            optionA = "Proximal convoluted tubule",
            optionB = "Descending limb of Loop of Henle",
            optionC = "Distal convoluted tubule",
            optionD = "Collecting duct",
            correctOption = "A",
            explanation = "Nearly 100% of filtered glucose and amino acids are reabsorbed in the proximal convoluted tubule via Na+-coupled secondary active transport.",
            youtubeUrl = "https://www.youtube.com/results?search_query=Nephron+Reabsorption+PCT+MDCAT+Biology",
            difficulty = "easy",
            status = "published"
        ),
        QuestionDraftInput(
            subject = "Biology",
            topic = "Genetics & Molecular Biology",
            stem = "During DNA replication, which enzyme synthesizes the new complementary DNA strand in the 5' to 3' direction?",
            optionA = "DNA helicase",
            optionB = "DNA ligase",
            optionC = "DNA polymerase III",
            optionD = "Topoisomerase",
            correctOption = "C",
            explanation = "DNA polymerase III adds deoxyribonucleotides to the free 3'-OH end of the primer, elongating the new strand in the 5' -> 3' direction.",
            youtubeUrl = "https://www.youtube.com/results?search_query=DNA+Replication+Polymerase+III+MDCAT+Biology",
            difficulty = "medium",
            status = "published"
        ),

        // --- CHEMISTRY (3) ---
        QuestionDraftInput(
            subject = "Chemistry",
            topic = "Chemical Bonding",
            stem = "Which molecule has a tetrahedral electron-pair geometry but a bent (V-shaped) molecular geometry with a bond angle of approximately 104.5°?",
            optionA = "CO2",
            optionB = "BF3",
            optionC = "H2O",
            optionD = "CH4",
            correctOption = "C",
            explanation = "Water (H2O) has two bonding pairs and two lone pairs on sp3-hybridized oxygen. Lone-pair repulsion compresses the H-O-H bond angle from 109.5° to 104.5°.",
            youtubeUrl = "https://www.youtube.com/results?search_query=VSEPR+Theory+H2O+Bond+Angle+MDCAT+Chemistry",
            difficulty = "easy",
            status = "published"
        ),
        QuestionDraftInput(
            subject = "Chemistry",
            topic = "Organic Chemistry",
            stem = "Which reaction mechanism proceeds via a two-step carbocation intermediate and yields a racemic mixture when starting from a chiral alkyl halide?",
            optionA = "SN1 substitution",
            optionB = "SN2 substitution",
            optionC = "E2 elimination",
            optionD = "Free-radical halogenation",
            correctOption = "A",
            explanation = "SN1 is a unimolecular two-step reaction forming a planar sp2 carbocation intermediate, allowing nucleophilic attack from both faces to yield racemization.",
            youtubeUrl = "https://www.youtube.com/results?search_query=SN1+vs+SN2+Mechanism+MDCAT+Chemistry",
            difficulty = "medium",
            status = "published"
        ),
        QuestionDraftInput(
            subject = "Chemistry",
            topic = "Chemical Equilibrium",
            stem = "According to Le Chatelier's principle, increasing the temperature of an exothermic equilibrium reaction shifts the equilibrium in which direction?",
            optionA = "Forward (toward products)",
            optionB = "Reverse (toward reactants)",
            optionC = "No shift in equilibrium position",
            optionD = "Increases both Kc and product yield",
            correctOption = "B",
            explanation = "Heat acts as a product in an exothermic reaction. Adding heat shifts equilibrium in the endothermic (reverse) direction, lowering Kc.",
            youtubeUrl = "https://www.youtube.com/results?search_query=Le+Chatelier+Principle+Exothermic+MDCAT+Chemistry",
            difficulty = "medium",
            status = "published"
        ),

        // --- PHYSICS (3) ---
        QuestionDraftInput(
            subject = "Physics",
            topic = "Work & Energy",
            stem = "A 2 kg ball is lifted vertically upward by 5 m near Earth's surface (g = 9.8 m/s²). What is the gain in gravitational potential energy?",
            optionA = "10 J",
            optionB = "49 J",
            optionC = "98 J",
            optionD = "196 J",
            correctOption = "C",
            explanation = "Gravitational potential energy ΔU = mgh = 2 kg × 9.8 m/s² × 5 m = 98 J.",
            youtubeUrl = "https://www.youtube.com/results?search_query=Work+and+Energy+Gravitational+Potential+Energy+MDCAT+Physics",
            difficulty = "easy",
            status = "published"
        ),
        QuestionDraftInput(
            subject = "Physics",
            topic = "Current Electricity",
            stem = "Three identical resistors of 6 Ω each are connected in parallel. What is their equivalent resistance?",
            optionA = "18 Ω",
            optionB = "6 Ω",
            optionC = "3 Ω",
            optionD = "2 Ω",
            correctOption = "D",
            explanation = "For n equal resistors in parallel, Req = R / n = 6 Ω / 3 = 2 Ω.",
            youtubeUrl = "https://www.youtube.com/results?search_query=Resistors+in+Parallel+MDCAT+Physics",
            difficulty = "easy",
            status = "published"
        ),
        QuestionDraftInput(
            subject = "Physics",
            topic = "Modern Physics",
            stem = "In the photoelectric effect, increasing the frequency of incident light above the threshold frequency directly increases which quantity?",
            optionA = "Work function of the metal",
            optionB = "Maximum kinetic energy of emitted photoelectrons",
            optionC = "Threshold wavelength of the metal",
            optionD = "Number of electrons emitted per second at fixed intensity",
            correctOption = "B",
            explanation = "By Einstein's photoelectric equation Kmax = hf - Φ, higher photon frequency increases Kmax linearly while the work function Φ remains a constant property of the metal.",
            youtubeUrl = "https://www.youtube.com/results?search_query=Photoelectric+Effect+MDCAT+Physics",
            difficulty = "medium",
            status = "published"
        ),

        // --- ENGLISH (3) ---
        QuestionDraftInput(
            subject = "English",
            topic = "Grammar & Agreement",
            stem = "Identify the grammatically correct sentence:",
            optionA = "Each of the medical interns have completed their rotation.",
            optionB = "Each of the medical interns has completed his or her rotation.",
            optionC = "Each of the medical intern have completed their rotation.",
            optionD = "Each of the medical interns are completing the rotation.",
            correctOption = "B",
            explanation = "'Each' is a singular indefinite pronoun and therefore requires the singular verb 'has' rather than 'have' or 'are'.",
            youtubeUrl = "https://www.youtube.com/results?search_query=Subject+Verb+Agreement+Each+MDCAT+English",
            difficulty = "easy",
            status = "published"
        ),
        QuestionDraftInput(
            subject = "English",
            topic = "Vocabulary",
            stem = "Choose the closest synonym for the word 'AMELIORATE' as used in clinical writing:",
            optionA = "Worsen",
            optionB = "Improve",
            optionC = "Ignore",
            optionD = "Postpone",
            correctOption = "B",
            explanation = "'Ameliorate' means to make a bad or painful condition better or more tolerable — synonymous with improve or alleviate.",
            youtubeUrl = "https://www.youtube.com/results?search_query=MDCAT+English+Vocabulary+Synonyms",
            difficulty = "medium",
            status = "published"
        ),
        QuestionDraftInput(
            subject = "English",
            topic = "Sentence Correction",
            stem = "Select the sentence that uses the conditional mood accurately:",
            optionA = "If she would have studied earlier, she would pass the test.",
            optionB = "If she had studied earlier, she would have passed the test.",
            optionC = "If she studied earlier, she would have passed the test.",
            optionD = "If she has studied earlier, she would have passed the test.",
            correctOption = "B",
            explanation = "The third conditional (past unreal situation) requires past perfect ('had studied') in the if-clause and 'would have + past participle' in the main clause.",
            youtubeUrl = "https://www.youtube.com/results?search_query=Third+Conditional+Sentences+MDCAT+English",
            difficulty = "hard",
            status = "published"
        ),

        // --- LOGICAL REASONING (3) ---
        QuestionDraftInput(
            subject = "Logical Reasoning",
            topic = "Number Series",
            stem = "Find the next number in the sequence: 3, 7, 15, 31, 63, ___",
            optionA = "95",
            optionB = "125",
            optionC = "127",
            optionD = "129",
            correctOption = "C",
            explanation = "Each term follows the pattern (previous term × 2) + 1: 3→7→15→31→63→(63×2)+1 = 127.",
            youtubeUrl = "https://www.youtube.com/results?search_query=Number+Series+MDCAT+Logical+Reasoning",
            difficulty = "easy",
            status = "published"
        ),
        QuestionDraftInput(
            subject = "Logical Reasoning",
            topic = "Logical Deduction",
            stem = "Statements: All surgeons are physicians. No physician is careless. Which conclusion MUST be true?",
            optionA = "Some surgeons are careless.",
            optionB = "No surgeon is careless.",
            optionC = "All physicians are surgeons.",
            optionD = "Some careless people are physicians.",
            correctOption = "B",
            explanation = "Because every surgeon is inside the set of physicians, and the set of physicians has zero overlap with careless individuals, no surgeon can be careless.",
            youtubeUrl = "https://www.youtube.com/results?search_query=Logical+Deduction+Syllogism+MDCAT",
            difficulty = "medium",
            status = "published"
        ),
        QuestionDraftInput(
            subject = "Logical Reasoning",
            topic = "Cause & Effect",
            stem = "Statement I: The city installed solar streetlights across all major avenues last month.\nStatement II: Nighttime traffic accidents dropped by 28% this month.\nWhich relationship is most plausible?",
            optionA = "Statement I is the likely cause and Statement II is its effect.",
            optionB = "Statement II is the cause and Statement I is its effect.",
            optionC = "Both statements are independent causes.",
            optionD = "Both statements are effects of an unrelated storm.",
            correctOption = "A",
            explanation = "Improved nighttime visibility from new solar streetlights precedes and logically explains the subsequent reduction in nighttime traffic accidents.",
            youtubeUrl = "https://www.youtube.com/results?search_query=Cause+and+Effect+MDCAT+Logical+Reasoning",
            difficulty = "medium",
            status = "published"
        )
    )
}
