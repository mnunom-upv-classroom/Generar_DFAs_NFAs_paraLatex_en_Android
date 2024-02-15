/*
* Integranetes del equipo:
* César Aldahir Flores Gámez
* Jose Alan Gonzalez Perales
* Vanessa Itzaiana Garcia Cervantes
* Anibal Gonzalez Tovar
* */

package upvictoria.pm_sep_dic_2023.iti_271086.pg1u1.eq_02

//Importaciones para el desarrollo del proyecto
import TransitionView
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class Node(
    val id: Int,
    val name: String,
    var isInitial: Boolean = false,
    var isFinal: Boolean = false,
    var x: Float = 0f,
    var y: Float = 0f
)

//se define una clase Node que representa un estado en un grafo. Puede ser un estado inicial (isInitial) y/o un estado final (isFinal).
data class Transition(
    val fromNode: Node,
    val toNode: Node,
    val value: String,
    var startX: Float = 0f,
    var startY: Float = 0f,
    var endX: Float = 0f,
    var endY: Float = 0f,
    var viewId: Int = 0
)

class MainActivity : AppCompatActivity() {
    //Variables
    private var stateCounter = 0
    private var lastTouchX: Float = 0f
    private var lastTouchY: Float = 0f
    private var isAddingInitialState = false
    private var isAddingFinalState = false
    private var isAddingTransition = false
    private var isAddingState = false
    private var isMoveModeEnabled = false
    private var initialViewState: StateView? = null
    private var firstSelectedState: StateView? = null
    private var secondSelectedState: StateView? = null
    private var initialNode: Node? = null
    private lateinit var stateContainer: RelativeLayout
    private val nodes: MutableList<Node> = mutableListOf()
    private val transitions: MutableList<Transition> = mutableListOf()
    private val finalNodes: MutableList<Node> = mutableListOf() // Lista para nodos finales
    private lateinit var exportActivityResultLauncher: ActivityResultLauncher<Intent>
    private val transitionCounts: MutableMap<Pair<String, String>, Int> = mutableMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //El método onCreate se ejecuta cuando se crea la actividad y se establece la vista principal.
        stateContainer = findViewById(R.id.containerLayout)
        //Se obtiene una referencia al contenedor de estados en la interfaz de usuario.
        val btnExportToLatex = findViewById<Button>(R.id.btnExportToLatex)
        val btnReset = findViewById<Button>(R.id.btnReset)
        val radioGroup = findViewById<RadioGroup>(R.id.radioGroup)

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.btnAddState -> {
                    toggleMode { isAddingState = !isAddingState }
                    deactivateOtherModes("state")
                    isMoveModeEnabled = false
                    disableMoveMode()
                }
                R.id.btnSetInitial -> {
                    toggleMode { isAddingInitialState = !isAddingInitialState }
                    deactivateOtherModes("initial")
                    isMoveModeEnabled = false
                    disableMoveMode()
                }
                R.id.btnSetFinal -> {
                    toggleMode { isAddingFinalState = !isAddingFinalState }
                    deactivateOtherModes("final")
                    isMoveModeEnabled = false
                    disableMoveMode()
                }
                R.id.btnAddTransition -> {
                    toggleMode { isAddingTransition = !isAddingTransition }
                    deactivateOtherModes("transition")
                    isMoveModeEnabled = false
                    disableMoveMode()
                }
                R.id.btnToggleMove -> {
                    isMoveModeEnabled = !isMoveModeEnabled
                    if (isMoveModeEnabled) {
                        // Desactiva todas las demás funcionalidades
                        isAddingState = false
                        isAddingInitialState = false
                        isAddingFinalState = false
                        isAddingTransition = false
                        // Activar el modo de movimiento
                        enableMoveMode()
                    } else {
                        // Desactivar el modo de movimiento
                        disableMoveMode()
                    }
                }
            }
        }

        btnReset.setOnClickListener {
            resetBoard(stateContainer)
        }


        //Funcion para exportar el contenido en formato latex
        exportActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                if (data != null) {
                    val uri = data.data
                    if (uri != null) {
                        // Escribir el código LaTeX en el archivo seleccionado
                        contentResolver.openOutputStream(uri)?.use { outputStream ->
                            val latexCode = buildLatexCode()
                            outputStream.write(latexCode.toByteArray())
                        }
                        Toast.makeText(this, "Archivo LaTeX exportado con éxito", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        //Btn para exporta a latex
        btnExportToLatex.setOnClickListener {
            buildLatexCode()

            // Abre un diálogo de selección de archivo
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "text/latex"
            intent.putExtra(Intent.EXTRA_TITLE, "archivo_latex.tex")

            exportActivityResultLauncher.launch(intent)  // Lanzar la actividad de selección de archivo
        }

        stateContainer.setOnTouchListener { _, event ->
            if (!isMoveModeEnabled && event.action == MotionEvent.ACTION_DOWN) {
                if (isAddingState) {
                    addState(stateContainer, event.x, event.y)
                }
                return@setOnTouchListener true
            }
            false
        }

    }

    private fun toggleMode(modeToggle: () -> Unit) {
        modeToggle()
    }

    private fun enableMoveMode() {
        for (i in 0 until stateContainer.childCount) {
            val child = stateContainer.getChildAt(i)
            if (child is StateView) {
                child.setOnTouchListener { view, motionEvent ->
                    if (isMoveModeEnabled) {
                        handleMove(view, motionEvent)
                    } else {
                        // Devuelve false para permitir que otros eventos de toque sean manejados
                        false
                    }
                }
            }
        }
    }

    private fun disableMoveMode() {
        for (i in 0 until stateContainer.childCount) {
            val child = stateContainer.getChildAt(i)
            if (child is StateView) {
                child.setOnTouchListener(null)
            }
        }
    }

    private fun handleMove(view: View, event: MotionEvent): Boolean{
        val layoutParams = view.layoutParams as RelativeLayout.LayoutParams

        val node = nodes.find { it.id == view.id }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.rawX
                lastTouchY = event.rawY
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.rawX - lastTouchX
                val deltaY = event.rawY - lastTouchY

                layoutParams.leftMargin += deltaX.toInt()
                layoutParams.topMargin += deltaY.toInt()
                view.layoutParams = layoutParams

                lastTouchX = event.rawX
                lastTouchY = event.rawY

                node?.let {
                    it.x = view.x
                    it.y = view.y
                }
            }
            MotionEvent.ACTION_UP -> {
                // Puedes agregar aquí cualquier lógica adicional al terminar el arrastre
            }
        }
        if (event.action == MotionEvent.ACTION_UP) {
            val node = nodes.find { it.id == view.id }
            node?.let { updateTransitionsForNode(it)
                println("Nodo ${it.name} movido a: x = ${view.x}, y = ${view.y}")}
        }
        return true
    }
    private fun printNodeCoordinates() {
        for (node in nodes) {
            println("Nodo ${node.name}: x = ${node.x}, y = ${node.y}")
        }
    }


    private fun updateTransitionsForNode(node: Node) {
        val relatedTransitions = transitions.filter {
            it.fromNode.id == node.id || it.toNode.id == node.id
        }

        for (transition in relatedTransitions) {
            val startView = findViewById<StateView>(transition.fromNode.id)
            val endView = findViewById<StateView>(transition.toNode.id)
            val transitionView = findViewById<TransitionView>(transition.viewId)

            if (transitionView != null) {
                // Calcular el ángulo de la línea entre los estados
                val deltaX = endView.x + endView.width / 2 - (startView.x + startView.width / 2)
                val deltaY = endView.y + endView.height / 2 - (startView.y + startView.height / 2)
                val angle = atan2(deltaY, deltaX)

                // Calcular nuevo punto de inicio ajustado
                val adjustedStartX = (startView.x + startView.width / 2) + 100f * cos(angle)
                val adjustedStartY = (startView.y + startView.height / 2) + 100f * sin(angle)

                transitionView.startX = adjustedStartX
                transitionView.startY = adjustedStartY
                transitionView.endX = endView.x + endView.width / 2
                transitionView.endY = endView.y + endView.height / 2

                transitionView.invalidate() // Redibuja la transición
            }
        }
    }


    private fun deactivateOtherModes(except: String) {
        if (except != "state") isAddingState = false
        if (except != "initial") isAddingInitialState = false
        if (except != "final") isAddingFinalState = false
        if (except != "transition") isAddingTransition = false
    }

    //Funcion para agregar el estado para llamar a eventos de dibujo cuando se toca la pantalla
    private fun addState(stateContainer: RelativeLayout, x: Float, y: Float) {
        val stateName = "q$stateCounter"

        // Verifica si un nodo con el mismo nombre ya existe en la lista
        if (nodes.any { it.name == stateName }) {
            return
        }

        val newState = StateView(this)
        val params = RelativeLayout.LayoutParams(200, 200)

        // Posicionar el estado en las coordenadas (x, y)
        params.leftMargin = (x - 100).toInt()
        params.topMargin = (y - 100).toInt()

        newState.layoutParams = params

        // Incrementa el contador de estados
        stateCounter++
        newState.stateName = stateName
        newState.id = View.generateViewId()
        stateContainer.addView(newState)

        val newNode = Node(newState.id, stateName, x = x, y = y)
        nodes.add(newNode)

        // Funcionalidad para agregar un nuevo nodo que representaa el estado y se agrega a la lista de nodos
        newState.setOnClickListener {
            // Marcando como estado inicial
            if (isAddingInitialState) {
                newState.isInitialState = !newState.isInitialState
                newNode.isInitial = newState.isInitialState
                if (newState.isInitialState) {
                    initialViewState?.let {
                        it.isInitialState = false
                        it.invalidate()
                    }
                    initialViewState = newState

                }
                nodes.forEach { it.isInitial = false }

                newState.isInitialState = true
                newNode.isInitial = true
                initialViewState = newState
                initialNode = Node(newState.id, newState.stateName, isInitial = true)
            }

            // Marcando como estado final
            if (isAddingFinalState) {
                newState.isFinalState = !newState.isFinalState
                newNode.isFinal = newState.isFinalState
                if (newState.isFinalState) {
                    finalNodes.add(Node(newState.id, newState.stateName, isFinal = true))
                } else {
                    finalNodes.removeIf { it.id == newState.id }
                }
            }

            // Agregando transiciones
            if (isAddingTransition) {
                if (firstSelectedState == null) {
                    firstSelectedState = newState
                    newState.isHighlighted = true
                } else {
                    if (secondSelectedState == null && firstSelectedState == newState) {
                        // Si el segundo nodo seleccionado es nulo y es el mismo que el primero,
                        // significa que queremos agregar un bucle
                        secondSelectedState = newState
                        newState.isHighlighted = true
                        drawSelfLoopTransition(firstSelectedState!!)
                    } else if (secondSelectedState == null && firstSelectedState != newState) {
                        // Si el segundo nodo seleccionado es nulo y no es el mismo que el primero,
                        // entonces es una transición entre dos nodos diferentes
                        secondSelectedState = newState
                        newState.isHighlighted = true
                        promptForTransitionValueAndDraw(firstSelectedState!!, secondSelectedState!!)
                    }
                }
            }
            newState.invalidate()  // Actualizar la representación visual del estado
        }
    }




    //Formato con el contenido del cuerpo de latex
    private fun buildLatexCode(): String {
        val latexBuilder = StringBuilder()
        latexBuilder.append("\\documentclass{standalone}\n")
        latexBuilder.append("\\usepackage{tikz}\n")
        latexBuilder.append("\\usetikzlibrary{automata, positioning, arrows, arrows.meta}\n")
        latexBuilder.append("\\begin{document}\n")
        latexBuilder.append("\\begin{tikzpicture}[\n")
        latexBuilder.append("    ->,\n")
        latexBuilder.append("    >=Stealth,\n")
        latexBuilder.append("]\n")

        // Calcular el tamaño del lienzo
        val canvasWidth = stateContainer.width
        val canvasHeight = stateContainer.height

        // Calcular la escala en base al tamaño del lienzo
        val scale = 10.0 / maxOf(canvasWidth, canvasHeight)  // Ajusta según tus necesidades

        // Ordenar los nodos por ID antes de imprimir
        val sortedNodes = nodes.sortedBy { it.id }

        for (node in sortedNodes) {
            val x = node.x * scale
            val y = (canvasHeight - node.y) * scale  // Invertir la coordenada y
            val nodeOptions = mutableListOf<String>()

            nodeOptions.add("state")  // Añadir siempre "state"

            if (node.isInitial) {
                nodeOptions.add("initial")
                println("Entre inicial")
            }

            if (finalNodes.any { it.id == node.id }) {
                nodeOptions.add("accepting")
            }

            val nodeOptionsString = nodeOptions.joinToString(", ")
            latexBuilder.append("\\node[$nodeOptionsString] (").append(node.name).append(") at (").append(x).append(",").append(y).append(") {").append("\$").append(node.name).append("\$};\n")
        }

        val processedTransitions = mutableSetOf<Pair<String, String>>()

        println("Transiciones:")
        for (transition in transitions) {
            println("${transition.fromNode.name} -> ${transition.toNode.name} : ${transition.value}")
        }
        val bendAngleIncrement = 10  // Ajusta según sea necesario
        var currentBendAngle = 0  // Ángulo inicial

        for ((index, transition) in transitions.withIndex()) {
            val fromNodeName = transition.fromNode.name
            val toNodeName = transition.toNode.name
            val key = Pair(fromNodeName, toNodeName)
            val reverseKey = Pair(toNodeName, fromNodeName)

            val count = transitionCounts.getOrDefault(key, 1)
            var bendDirection = when (count) {
                1 -> if (index == 0) "" else "bend left" // La primera es directa, las siguientes curvadas
                2 -> "bend left"
                else -> "bend right"
            }

            if (processedTransitions.contains(reverseKey)) {
                bendDirection = "bend right"
            }

            val yOffset = index * 0.5  // Ajusta según sea necesario

            if (fromNodeName == toNodeName) {
                val label = transition.value
                // Calcular el ángulo de la transición para los bucles
                val angle = 90.0 + (processedTransitions.count { it.first == fromNodeName && it.second == toNodeName } * 15.0)
                val yOffsetLoop = 0.2 * Math.sin(Math.toRadians(angle))

                latexBuilder.append("\\draw (").append(fromNodeName)
                    .append(") edge[loop above, right, yshift=$yOffsetLoop] node{").append(label).append("} (").append(toNodeName).append(");\n")
            } else {
                // Calcular el ángulo de la transición para las transiciones normales
                val deltaX = transition.toNode.x - transition.fromNode.x
                val deltaY = transition.toNode.y - transition.fromNode.y
                val angle = Math.toDegrees(Math.atan2(deltaY.toDouble(), deltaX.toDouble()))

                // Añadir un pequeño desplazamiento vertical
                val yOffsetNormal = index * 0.2

                if (bendDirection.isNotEmpty()) {
                    // Solo agregar el parámetro de curvatura si la transición es curva
                    latexBuilder.append("\\draw (").append(fromNodeName).append(") edge[$bendDirection=$currentBendAngle, yshift=$yOffsetNormal] node{")
                        .append(transition.value).append("} (").append(toNodeName).append(");\n")
                } else {
                    // Transición recta
                    latexBuilder.append("\\draw (").append(fromNodeName).append(") edge[] node{")
                        .append(transition.value).append("} (").append(toNodeName).append(");\n")
                }
            }

            processedTransitions.add(key)
            currentBendAngle += bendAngleIncrement
        }







        // Imprimir el código LaTeX en la consola antes de guardarlo
        val latexCode = latexBuilder.toString()
        println("Código LaTeX:")
        println(latexCode)

        latexBuilder.append("\\end{tikzpicture}\n")
        latexBuilder.append("\\end{document}\n")
        return latexBuilder.toString()
    }




    //limpiar el tablero en caso de querer hacer nuevas pruebas
    private fun resetBoard(stateContainer: RelativeLayout) {
        stateContainer.removeAllViews()
        stateCounter = 0
        initialViewState = null
        isAddingInitialState = false
        isAddingFinalState = false
        isAddingTransition = false

        // Limpia las listas de nodos y transiciones
        nodes.clear()
        transitions.clear()
        initialNode = null
        finalNodes.clear()

    }

    // Insertar una nueva trancisión (incluyendo autoloops)
    private fun promptForTransitionValueAndDraw(startState: StateView, endState: StateView) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Ingrese el valor de la transición")

        val input = EditText(this)
        builder.setView(input)

        builder.setPositiveButton("OK") { dialog, _ ->
            val transitionValue = input.text.toString().trim()
            if (transitionValue.isNotEmpty()) {
                // Verificar si la transición ya existe
                val existingTransition = transitions.find {
                    it.fromNode == Node(startState.id, startState.stateName) &&
                            it.toNode == Node(endState.id, endState.stateName) &&
                            it.value == transitionValue
                }

                if (existingTransition == null) {
                    // Dibujar la transición solo si no existe
                    drawTransition(startState, endState, transitionValue)
                    // Agregar la nueva transición solo si no exist
                }
            }

            // Restablecer la selección
            firstSelectedState?.isHighlighted = false
            secondSelectedState?.isHighlighted = false
            firstSelectedState = null
            secondSelectedState = null
            dialog.dismiss()
        }

        // Cancelar la entrada de transiciones
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            // Restablecer la selección
            firstSelectedState?.isHighlighted = false
            secondSelectedState?.isHighlighted = false
            firstSelectedState = null
            secondSelectedState = null
            dialog.cancel()
        }

        builder.setCancelable(false) // Evitar que el diálogo se cierre al tocar fuera de él

        val alertDialog = builder.create()
        alertDialog.show()

        // Modificación: Habilitar el botón OK solo si hay texto en el campo de entrada
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Habilitar el botón OK solo si hay texto en el campo de entrada
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = !s.isNullOrBlank()
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }




    private fun drawTransition(startState: StateView, endState: StateView, value: String) {
        // Verificar si la transición ya existe en la lista transitions
        val existingTransition = transitions.find {
            it.fromNode == Node(startState.id, startState.stateName) &&
                    it.toNode == Node(endState.id, endState.stateName) &&
                    it.value == value
        }

        val existingTransitionView = existingTransition?.let {
            stateContainer.findViewById<TransitionView>(it.viewId)
        }


        if (existingTransition == null) {
            // Verificar si la transición ya existe en la lista transitionViews
            if (existingTransitionView == null) {
                val transitionView = TransitionView(this)

                // Calcular el ángulo de la línea entre los estados
                val deltaX = endState.x + endState.width / 2 - (startState.x + startState.width / 2)
                val deltaY = endState.y + endState.height / 2 - (startState.y + startState.height / 2)
                val angle = atan2(deltaY, deltaX)

                // Calcular nuevo punto de inicio
                val adjustedStartX = (startState.x + startState.width / 2) + 100f * cos(angle)
                val adjustedStartY = (startState.y + startState.height / 2) + 100f * sin(angle)

                if (startState == endState) {
                    // Transición autoreferenciada.
                    val offset = 150  // Ajusta según necesites.
                    transitionView.startX = adjustedStartX - offset
                    transitionView.startY = adjustedStartY - offset
                    transitionView.endX = adjustedStartX + offset
                    transitionView.endY = adjustedStartY + offset
                } else {
                    transitionView.startX = adjustedStartX
                    transitionView.startY = adjustedStartY
                    transitionView.endX = endState.x + endState.width / 2
                    transitionView.endY = endState.y + endState.height / 2
                }

                transitionView.transitionValue = value

                val params = RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                transitionView.layoutParams = params

                // Guardar la transición con su nuevo ID
                val newViewId = View.generateViewId()
                transitionView.id = newViewId
                stateContainer.addView(transitionView)

                // Agregar la nueva transición a ambas listas
                transitions.add(Transition(
                    Node(startState.id, startState.stateName),
                    Node(endState.id, endState.stateName),
                    value,
                    startX = transitionView.startX,
                    startY = transitionView.startY,
                    endX = transitionView.endX,
                    endY = transitionView.endY,
                    viewId = newViewId
                ))
            }
        }
    }



    private fun drawSelfLoopTransition(state: StateView) {
        try {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Enter Transition Value")

            val input = EditText(this)
            builder.setView(input)

            builder.setPositiveButton("OK") { dialog, _ ->
                val transitionValue = input.text.toString()
                if (transitionValue.isNotEmpty()) {
                    val loop = TransitionView(this)
                    loop.selfLoopCounter += 1
                    loop.setupSelfLoop(state, state, transitionValue)
                    stateContainer.addView(loop)
                    // Add the recursive transition to the transitions list
                    val newTransition = Transition(
                        Node(state.id, state.stateName),
                        Node(state.id, state.stateName),
                        transitionValue
                    )
                    transitions.add(newTransition)
                }
                dialog.dismiss()
                firstSelectedState?.isHighlighted = false
                firstSelectedState = null
                // Reset secondSelectedState to null after creating the self-transition
                secondSelectedState = null
            }

            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
                firstSelectedState?.isHighlighted = false
                firstSelectedState = null
                // Reset secondSelectedState to null if creating the transition is canceled
                secondSelectedState = null
            }

            builder.setCancelable(false) // Prevent dialog from closing on outside touch

            val alertDialog = builder.create()
            alertDialog.show()
        } catch (e: Exception) {
            e.printStackTrace()
            // Handle exception or show error message here
        }
    }
}
