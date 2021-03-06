package ch.hearc.android.sucle.view;

import java.util.Date;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import ch.hearc.android.sucle.R;
import ch.hearc.android.sucle.Sucle;
import ch.hearc.android.sucle.Tools;
import ch.hearc.android.sucle.controller.PostsManager;
import ch.hearc.android.sucle.model.Post;

import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.model.Marker;

public class PostInfoWindowAdapter implements InfoWindowAdapter
{
	private View view;
	
	public PostInfoWindowAdapter(Context context)
	{
		LayoutInflater inflater = LayoutInflater.from(context);
		view = inflater.inflate(R.layout.timeline_row_fragment, null);
	}

	@Override
	public View getInfoContents(Marker marker)
	{
		return null;
	}

	@Override
	public View getInfoWindow(Marker marker)
	{
		Post post = PostsManager.getInstance().getPosts().get(Integer.parseInt(marker.getTitle()));
		view.setBackgroundColor(Sucle.getAppContext().getResources().getColor(R.color.white));

		ProfilePictureView userImageViewFB = (ProfilePictureView) view.findViewById(R.id.profilePictureViewFB);
		RoundedImageView userImageViewGP = (RoundedImageView) view.findViewById(R.id.profilePictureViewGP);
		TextView username = (TextView) view.findViewById(R.id.username);
		TextView postContent = (TextView) view.findViewById(R.id.postContent);
		TextView location = (TextView) view.findViewById(R.id.location);
		TextView postDate = (TextView) view.findViewById(R.id.postDate);
		ImageView attachmentImageView = (ImageView) view.findViewById(R.id.attachmentImageView);

		long delta = ((new Date()).getTime() - post.getTime().getTime());
		username.setText(post.getUser().getSocialId());
		username.setText(post.getUser().getName());
		postContent.setText(post.getMessage());
		location.setText(post.getPositionName());
		postDate.setText(Tools.ago(delta));
		if (post.getAttachment() != null)
		{
			switch (post.getAttachment().getAttachementType())
			{
				case Picture:
					attachmentImageView.setImageResource(R.drawable.ic_camera);
					break;
				case Video:
					attachmentImageView.setImageResource(R.drawable.ic_video);
					break;
				case Sound:
					attachmentImageView.setImageResource(R.drawable.ic_sound);
					break;
				default:
					break;
			}
			attachmentImageView.setVisibility(View.VISIBLE);
		}
		else
		{
			attachmentImageView.setVisibility(View.GONE);
		}

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

		return view;
	}
}
