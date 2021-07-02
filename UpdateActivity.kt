package br.edu.infnet.todolist.ui.update

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import br.edu.infnet.todolist.R
import br.edu.infnet.todolist.domain.`interface`.IPaisService
import br.edu.infnet.todolist.domain.models.pais.Paises
import br.edu.infnet.todolist.domain.models.task.Task
import br.edu.infnet.todolist.domain.service.RetrofitService
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_create.*
import kotlinx.android.synthetic.main.activity_update.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UpdateActivity : AppCompatActivity() {
    private var TAG = "UpdateActivity"
    private lateinit var db: FirebaseFirestore
    private var toolbar: Toolbar? = null
    private var userId = ""
    private var id = ""
    private lateinit var spinner: Spinner
    private lateinit var progressbar: ProgressBar

    private var NAME_VALIDATION_MSG = "Campo título não pode ficar em branco."
    private var DESCRIPTION_VALIDATION_MSG = "Campo descrição não pode ficar em branco."

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update)

        toolbar = findViewById(R.id.toolbarUpdate)
        spinner = findViewById(R.id.spinnerUpdate)
        progressbar = findViewById(R.id.progressbarUpdateActivity)

        id = intent.extras?.getString("id")!!

        val user = Firebase.auth.currentUser
        for (profile in user?.providerData!!) {
            userId = profile.uid
        }

        db = FirebaseFirestore.getInstance()
        configActionBar()
        getCountries()

        updateTask.setOnClickListener {
            updateTask()
        }

    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    private fun getCountries() {
        progressbar.visibility = View.VISIBLE

        val call = RetrofitService.getInstance().create(IPaisService::class.java)
        call.getAllCountries().enqueue(object : Callback<ArrayList<Paises>> {
            override fun onResponse(
                call: Call<ArrayList<Paises>>,
                response: Response<ArrayList<Paises>>
            ) {
                val paises = response.body()!!
                val nome: ArrayList<String> = ArrayList()

                paises.forEach {
                    nome.add(it.nome.abreviado)
                }

                spinner.adapter = ArrayAdapter(
                    this@UpdateActivity,
                    R.layout.support_simple_spinner_dropdown_item,
                    nome
                )
                progressbar.visibility = View.GONE
            }

            override fun onFailure(call: Call<ArrayList<Paises>>, t: Throwable) {
                makeText("Falha ao listar os países.")
                Log.e(TAG, "onFailure: ${t.message}")
                progressbar.visibility = View.GONE
            }
        })
    }

    private fun registerTask() {
        val task = Task()

        task.title = editTitleUpdate.text.toString()
        task.description = editDescriptionUpdate.text.toString()
        task.country = spinner.selectedItem.toString()

        if (editTitle.text.isEmpty()) {
            setError(editTitle, NAME_VALIDATION_MSG)
            editTitle.requestFocus()
        }

        if (editDescription.text.isEmpty()) {
            setError(editDescription, DESCRIPTION_VALIDATION_MSG)
            editDescription.requestFocus()
        }

        if (task.title.isNotEmpty() && task.description.isNotEmpty()) {
            updateTask(id)
        }
    }

    private fun configActionBar() {
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun makeText(text: String) {
        Toast.makeText(applicationContext, text, Toast.LENGTH_LONG).show()
    }

    private fun setError(str: EditText, error: String) {
        return str.setError(error)
    }

    private fun updateTask(id: String) {
        var title = editTitle.text.toString()
        var description = editDescription.text.toString()
        var country = spinner.selectedItem.toString()

        db.collection(userId).document(id)
            .addSnapshotListener(EventListener{ document, e -> if(e != null) return@EventListener

                val tarefas = ArrayList<Task>()
                document?.let {
                    for(doc in it) {
                        val tarefa = doc.toObject(Task::class.java)
                        tarefas.add(tarefa)
                    }
                }

                for (tarefa in tarefas) {
                    title = tarefa.title
                    description = tarefa.description
                    country = tarefa.country
                }
            })
    }
}