package cd.ghost.detailtask

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import cd.ghost.common.base.BaseScreen
import cd.ghost.common.base.args
import cd.ghost.common.helper.viewBinding
import cd.ghost.detailtask.databinding.FragmentDetailTaskBinding
import cd.ghost.detailtask.di.DetailTaskComponentProvider
import javax.inject.Inject

class DetailTaskFragment : Fragment(R.layout.fragment_detail_task) {

    class DetailTask(
        val taskId: String
    ) : BaseScreen

    private val binding by viewBinding<FragmentDetailTaskBinding>()

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    private val viewModel by viewModels<DetailTaskViewModel> { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        (requireActivity() as DetailTaskComponentProvider)
            .provideDetailTaskSubcomponent()
            .create()
            .inject(this)

        val task = args<DetailTask>().taskId
        viewModel.initTask(task)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }
}