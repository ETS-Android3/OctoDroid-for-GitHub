/*
 * Copyright 2011 Azwan Adli Abdullah
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh4a.fragment;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.service.RepositoryService;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;

import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.adapter.RepositoryAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.IntentUtils;

public class RepositoryListFragment extends PagedDataBaseFragment<Repository> {
    private String mLogin;
    private String mRepoType;
    private boolean mIsOrg;
    private String mSortOrder;
    private String mSortDirection;

    public static RepositoryListFragment newInstance(String login, String userType,
            String repoType, String sortOrder, String sortDirection) {
        RepositoryListFragment f = new RepositoryListFragment();

        Bundle args = new Bundle();
        args.putString(Constants.User.LOGIN, login);
        args.putString(Constants.User.TYPE, userType);
        args.putString(Constants.Repository.TYPE, repoType);
        args.putString("sortOrder", sortOrder);
        args.putString("sortDirection", sortDirection);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLogin = getArguments().getString(Constants.User.LOGIN);
        mRepoType = getArguments().getString(Constants.Repository.TYPE);
        mIsOrg = Constants.User.TYPE_ORG.equals(getArguments().getString(Constants.User.TYPE));
        mSortOrder = getArguments().getString("sortOrder");
        mSortDirection = getArguments().getString("sortDirection");
    }

    @Override
    protected RootAdapter<Repository, ? extends RecyclerView.ViewHolder> onCreateAdapter() {
        return new RepositoryAdapter(getActivity());
    }

    @Override
    protected int getEmptyTextResId() {
        return R.string.no_repos_found;
    }

    @Override
    protected void onAddData(RootAdapter<Repository, ? extends RecyclerView.ViewHolder> adapter,
            Collection<Repository> repositories) {
        if (!mIsOrg && ("sources".equals(mRepoType) || "forks".equals(mRepoType))) {
            for (Repository repository : repositories) {
                if ("sources".equals(mRepoType) && !repository.isFork()) {
                    adapter.add(repository);
                } else if ("forks".equals(mRepoType) && repository.isFork()) {
                    adapter.add(repository);
                }
            }
            adapter.notifyDataSetChanged();
        } else {
            adapter.addAll(repositories);
        }
    }

    @Override
    public void onItemClick(Repository repository) {
        IntentUtils.openRepositoryInfoActivity(getActivity(), repository);
    }

    @Override
    protected PageIterator<Repository> onCreateIterator() {
        Gh4Application app = Gh4Application.get();
        boolean isSelf = ApiHelpers.loginEquals(mLogin, app.getAuthLogin());
        RepositoryService repoService = (RepositoryService) app.getService(Gh4Application.REPO_SERVICE);

        Map<String, String> filterData = new HashMap<>();
        if (!mIsOrg && ("sources".equals(mRepoType) || "forks".equals(mRepoType))) {
            filterData.put("type", "all");
        } else if (isSelf && "all".equals(mRepoType)) {
            filterData.put("affiliation", "owner,collaborator");
        } else {
            filterData.put("type", mRepoType);
        }
        if (mSortOrder != null) {
            filterData.put("sort", mSortOrder);
            filterData.put("direction", mSortDirection);
        }

        if (isSelf) {
            return repoService.pageRepositories(filterData);
        }
        return repoService.pageRepositories(mLogin, filterData);
    }
}