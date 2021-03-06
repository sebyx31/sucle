package ch.hearc.android.sucle.view;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import android.app.Fragment;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.VideoView;
import ch.hearc.android.sucle.R;
import ch.hearc.android.sucle.controller.FetchCommentsTask.FetchCommentsListener;
import ch.hearc.android.sucle.controller.PostsManager;
import ch.hearc.android.sucle.model.Attachment.ImageViewInfo;
import ch.hearc.android.sucle.model.Post;

public class PostDetailsFragment extends Fragment implements FetchCommentsListener
{
	public static final int		NO_POST			= -1;
	public final static String	ARG_POSITION	= "position";
	private int					currentPosition	= NO_POST;
	private View				view;
	private ImageView			imageView;
	private VideoView			videoView;
	private MediaPlayer			mediaPlayer;
	private Post				post;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{

		// If activity recreated (such as from screen rotate), restore
		// the previous article selection set by onSaveInstanceState().
		// This is primarily necessary when in the two-pane layout.
		if (savedInstanceState != null)
		{
			currentPosition = savedInstanceState.getInt(ARG_POSITION);
		}
		// Inflate the layout for this fragment
		view = inflater.inflate(R.layout.post_details_fragment, container, false);

		Button addCommentButton = (Button) view.findViewById(R.id.addComment);
		addCommentButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0)
			{
				Intent intent = new Intent(getActivity(), NewMessageActivity.class);
				intent.putExtra("location", PostsManager.getInstance().getLocation());
				intent.putExtra("deviceId", MainActivity.mDeviceId);
				intent.putExtra("token", MainActivity.mToken);
				intent.putExtra("parent", post.getId());
				startActivity(intent);
			}
		});

		return view;
	}

	@Override
	public void onStart()
	{
		super.onStart();

		PostsManager.getInstance().setListenerComment(this);

		Bundle args = getArguments();
		if (args != null)
		{
			updatePostView(args.getInt(ARG_POSITION));
		}
		else if (currentPosition != NO_POST)
		{
			updatePostView(currentPosition);
		}
	}

	@Override
	public void onStop()
	{
		super.onStop();
		stopSound();
	}

	@Override
	public void onPause()
	{
		super.onPause();
		PostsManager.getInstance().setListenerComment(null);
	}

	@Override
	public void onResume()
	{
		super.onResume();
		if (post != null) PostsManager.getInstance().getComments(post.getId());
	}

	public void updatePostView(int position)
	{
		if (position == NO_POST)
		{
			cleanView();
			return;
		}
		if (position == currentPosition) return;
		
		cleanView();
		
		currentPosition = position;

		post = PostsManager.getInstance().getPosts().get(position);

		PostsManager.getInstance().setListenerComment(this);
		PostsManager.getInstance().getComments(post.getId());

		TextView postContent = (TextView) view.findViewById(R.id.postContentDetails);
		TextView profileName = (TextView) view.findViewById(R.id.profileName);

		view.findViewById(R.id.addComment).setVisibility(View.VISIBLE);

		ProfilePictureView userImageViewFB = (ProfilePictureView) view.findViewById(R.id.profilePictureViewFB);
		RoundedImageView userImageViewGP = (RoundedImageView) view.findViewById(R.id.profilePictureViewGP);

		switch (post.getUser().getSocialType())
		{
			case Facebook:
				userImageViewFB.setVisibility(View.VISIBLE);
				userImageViewGP.setVisibility(View.GONE);
				userImageViewFB.setProfileId(post.getUser().getSocialId());
				break;
			case GooglePlus:
				userImageViewFB.setVisibility(View.GONE);
				userImageViewGP.setVisibility(View.VISIBLE);
				userImageViewGP.setImageBitmap(post.getUser().getImage());
				break;

			default:
				break;
		}

		profileName.setText(post.getUser().getName());
		postContent.setText(post.getMessage());
		
		if (post.getAttachment() != null)
		{
			showIsLoading(true);
			switch (post.getAttachment().getAttachementType())
			{
				case Picture:
					if (imageView == null)
					{
						ViewGroup layout = (ViewGroup) view.findViewById(R.id.attachmentLayout);
						imageView = new ImageView(getActivity());
						android.widget.LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, 300);
						layoutParams.gravity = Gravity.CENTER;
						imageView.setLayoutParams(layoutParams);
						layout.addView(imageView);
					}
					post.getAttachment().loadImage(imageView, new ImageViewInfo() {
						
						@Override
						public void onImageLoaded()
						{
							showIsLoading(false);
						}
					});
					imageView.setVisibility(View.VISIBLE);
					break;
				case Video:
					if (videoView == null)
					{
						ViewGroup layout = (ViewGroup) view.findViewById(R.id.attachmentLayout);
						videoView = new VideoView(getActivity());
						videoView.setVisibility(View.GONE);
						// videoView.setMediaController(new
						// MediaController(getActivity()));
						videoView.requestFocus();
						videoView.start();
						videoView.setOnTouchListener(new OnTouchListener() {

							@Override
							public boolean onTouch(View v, MotionEvent event)
							{
								if (videoView.isPlaying())
									videoView.pause();
								else
									videoView.start();
								return false;
							}
						});
						videoView.setOnPreparedListener(new OnPreparedListener() {
							@Override
							public void onPrepared(MediaPlayer mp)
							{
								mp.setLooping(true);
								videoView.start();
							}
						});
						layout.addView(videoView);
					}

					playVideoForPath(post.getAttachment().getFilePath());
					break;
				case Sound:
					final ImageView playPause = (ImageView) view.findViewById(R.id.playPauseImageView);
					playPause.setImageResource(R.drawable.ic_pause);
					new Thread(new Runnable() {

						@Override
						public void run()
						{
							mediaPlayer = MediaPlayer.create(getActivity(), Uri.parse(post.getAttachment().getFilePath()));
							if(mediaPlayer == null) return;
							mediaPlayer.setLooping(true);
							mediaPlayer.start();
							if(getActivity() == null) return;
							getActivity().runOnUiThread(new Runnable() {

								@Override
								public void run()
								{
									showIsLoading(false);
									playPause.setVisibility(View.VISIBLE);
								}
							});
						}
					}).start();
					playPause.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View view)
						{
							if (mediaPlayer.isPlaying())
							{
								mediaPlayer.pause();
								playPause.setImageResource(R.drawable.ic_play);
							}
							else
							{
								mediaPlayer.start();
								playPause.setImageResource(R.drawable.ic_pause);
							}
						}
					});
					break;
				default:
					break;
			}
		}
	}

	private void cleanView()
	{
		post = null;

		((LinearLayout) (view.findViewById(R.id.commentsFragment))).removeAllViews();

		TextView postContent = (TextView) view.findViewById(R.id.postContentDetails);
		TextView profileName = (TextView) view.findViewById(R.id.profileName);
		profileName.setText("");
		postContent.setText("");

		view.findViewById(R.id.addComment).setVisibility(View.GONE);
		view.findViewById(R.id.playPauseImageView).setVisibility(View.GONE);

		ProfilePictureView userImageViewFB = (ProfilePictureView) view.findViewById(R.id.profilePictureViewFB);
		RoundedImageView userImageViewGP = (RoundedImageView) view.findViewById(R.id.profilePictureViewGP);

		userImageViewGP.setVisibility(View.GONE);
		userImageViewFB.setVisibility(View.GONE);

		if (imageView != null) {
			imageView.setImageDrawable(null);
			imageView.setVisibility(View.GONE);
		}
		if (videoView != null) {
			videoView.stopPlayback();
			videoView.setVisibility(View.GONE);
		}
		stopSound();
	}

	private void stopSound()
	{
		try
		{
			if (mediaPlayer != null && mediaPlayer.isPlaying())
			{
				mediaPlayer.stop();
				mediaPlayer.release();
				mediaPlayer.reset();
			}
		}
		catch (Exception e)
		{
			// nothing to do
		}
	}

	private void playVideoForPath(final String path)
	{
		if (!URLUtil.isNetworkUrl(path))
		{
			playVideo(path);
		}
		else
		{
			new Thread(new Runnable() {

				@Override
				public void run()
				{
					try
					{
						URL url = new URL(path);
						URLConnection cn = url.openConnection();
						cn.connect();
						InputStream stream = cn.getInputStream();
						if (stream == null) throw new RuntimeException("stream is null");
						File temp = File.createTempFile("mediaplayertmp", "dat");
						temp.deleteOnExit();
						String tempPath = temp.getAbsolutePath();
						FileOutputStream out = new FileOutputStream(temp);
						byte buf[] = new byte[128];
						do
						{
							int numread = stream.read(buf);
							if (numread <= 0) break;
							out.write(buf, 0, numread);
						} while (true);
						out.close();
						stream.close();
						playVideo(tempPath);
					}
					catch (IOException e)
					{
						Log.e(TAG, e.getMessage());
					}
				}
			}).start();
		}
	}

	private void playVideo(final String path)
	{
		// The fragment is no longer displayed 
		if(getActivity() == null) return;
		getActivity().runOnUiThread(new Runnable() {

			@Override
			public void run()
			{
				videoView.setVideoPath(path);
				videoView.requestFocus();
				videoView.setVisibility(View.VISIBLE);
				android.widget.LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, 300);
				layoutParams.gravity = Gravity.CENTER;
				videoView.setLayoutParams(layoutParams);
				showIsLoading(false);
			}
		});
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);

		// Save the current article selection in case we need to recreate the
		// fragment
		outState.putInt(ARG_POSITION, currentPosition);
	}

	private static final String	TAG	= PostDetailsFragment.class.getSimpleName();

	@Override
	public void onCommentsFetched()
	{
		((LinearLayout) (view.findViewById(R.id.commentsFragment))).removeAllViews();
		try
		{
			Post[] comments = PostsManager.getInstance().getComments();
			for (int i = 0; i < comments.length; ++i)
			{
				if (comments[i].getParent() != post.getId()) continue;
				CommentFragment commentFragment = new CommentFragment();
				commentFragment.setPost(comments[i]);
				getFragmentManager().beginTransaction().add(R.id.commentsFragment, commentFragment, "comment" + comments[i].getId()).commit();
			}
		}
		catch (NullPointerException e)
		{

		}
	}

	private void showIsLoading(boolean show)
	{
		if (show)
			view.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
		else
			view.findViewById(R.id.progressBar).setVisibility(View.GONE);

	}
}
