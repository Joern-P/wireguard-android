/*
 * Copyright © 2017-2019 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android.fragment;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.wireguard.android.Application;
import com.wireguard.android.R;
import com.wireguard.android.activity.BaseActivity;
import com.wireguard.android.activity.BaseActivity.OnSelectedTunnelChangedListener;
import com.wireguard.android.backend.GoBackend;
import com.wireguard.android.backend.Tunnel.State;
import com.wireguard.android.databinding.TunnelDetailFragmentBinding;
import com.wireguard.android.databinding.TunnelListItemBinding;
import com.wireguard.android.model.ObservableTunnel;
import com.wireguard.android.util.ErrorMessages;
import com.wireguard.util.NonNullForAll;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.Fragment;

/**
 * Base class for fragments that need to know the currently-selected tunnel. Only does anything when
 * attached to a {@code BaseActivity}.
 */

@NonNullForAll
public abstract class BaseFragment extends Fragment implements OnSelectedTunnelChangedListener {
    private static final int REQUEST_CODE_VPN_PERMISSION = 23491;
    private static final String TAG = "WireGuard/" + BaseFragment.class.getSimpleName();
    @Nullable private BaseActivity activity;
    @Nullable private ObservableTunnel pendingTunnel;
    @Nullable private Boolean pendingTunnelUp;

    @Nullable
    protected ObservableTunnel getSelectedTunnel() {
        return activity != null ? activity.getSelectedTunnel() : null;
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, @Nullable final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_VPN_PERMISSION) {
            if (pendingTunnel != null && pendingTunnelUp != null)
                setTunnelStateWithPermissionsResult(pendingTunnel, pendingTunnelUp);
            pendingTunnel = null;
            pendingTunnelUp = null;
        }
    }

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);
        if (context instanceof BaseActivity) {
            activity = (BaseActivity) context;
            activity.addOnSelectedTunnelChangedListener(this);
        } else {
            activity = null;
        }
    }

    @Override
    public void onDetach() {
        if (activity != null)
            activity.removeOnSelectedTunnelChangedListener(this);
        activity = null;
        super.onDetach();
    }

    protected void setSelectedTunnel(@Nullable final ObservableTunnel tunnel) {
        if (activity != null)
            activity.setSelectedTunnel(tunnel);
    }

    public void setTunnelState(final View view, final boolean checked) {
        final ViewDataBinding binding = DataBindingUtil.findBinding(view);
        final ObservableTunnel tunnel;
        if (binding instanceof TunnelDetailFragmentBinding)
            tunnel = ((TunnelDetailFragmentBinding) binding).getTunnel();
        else if (binding instanceof TunnelListItemBinding)
            tunnel = ((TunnelListItemBinding) binding).getItem();
        else
            return;
        if (tunnel == null)
            return;

        Application.getBackendAsync().thenAccept(backend -> {
            if (backend instanceof GoBackend) {
                final Intent intent = GoBackend.VpnService.prepare(view.getContext());
                if (intent != null) {
                    pendingTunnel = tunnel;
                    pendingTunnelUp = checked;
                    startActivityForResult(intent, REQUEST_CODE_VPN_PERMISSION);
                    return;
                }
            }

            setTunnelStateWithPermissionsResult(tunnel, checked);
        });
    }

    private void setTunnelStateWithPermissionsResult(final ObservableTunnel tunnel, final boolean checked) {
        tunnel.setState(State.of(checked)).whenComplete((state, throwable) -> {
            if (throwable == null)
                return;
            final String error = ErrorMessages.get(throwable);
            final int messageResId = checked ? R.string.error_up : R.string.error_down;
            final String message = requireContext().getString(messageResId, error);
            final View view = getView();
            if (view != null)
                Snackbar.make(view, message, Snackbar.LENGTH_LONG).setAnchorView(view.findViewById(R.id.create_fab)).show();
            else
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            Log.e(TAG, message, throwable);
        });
    }

}
