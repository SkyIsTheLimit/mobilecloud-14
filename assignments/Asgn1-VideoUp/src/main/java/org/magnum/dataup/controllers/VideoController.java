package org.magnum.dataup.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.magnum.dataup.VideoFileManager;
import org.magnum.dataup.VideoSvcApi;
import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class VideoController {
	private List<Video> videos;
	private AtomicLong atomic;

	private static VideoFileManager videoDataMgr;

	static {
		try {
			videoDataMgr = VideoFileManager.get();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public VideoController() {
		this.videos = new ArrayList<Video>();
		this.atomic = new AtomicLong(0L);
	}

	@RequestMapping(value = VideoSvcApi.VIDEO_SVC_PATH, method = RequestMethod.GET)
	public @ResponseBody
	Collection<Video> getVideoList() {
		return this.videos;
	}

	private String getDataUrl(long videoId) {
		String url = getUrlBaseForLocalServer() + "/video/" + videoId + "/data";
		return url;
	}

	private String getUrlBaseForLocalServer() {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
				.getRequestAttributes()).getRequest();
		String base = "http://"
				+ request.getServerName()
				+ ((request.getServerPort() != 80) ? ":"
						+ request.getServerPort() : "");
		return base;
	}

	@RequestMapping(value = VideoSvcApi.VIDEO_SVC_PATH, method = RequestMethod.POST)
	public @ResponseBody
	Video addVideo(@RequestBody Video v) {
		v.setId(atomic.incrementAndGet());
		v.setDataUrl(this.getDataUrl(v.getId()));
		this.videos.add(v);

		return v;
	}

	@RequestMapping(value = VideoSvcApi.VIDEO_DATA_PATH, method = RequestMethod.POST)
	public @ResponseBody
	VideoStatus setVideoData(@PathVariable(VideoSvcApi.ID_PARAMETER) long id,
			@RequestParam("data") MultipartFile videoData,
			HttpServletResponse response) {
		for (Video video : this.videos) {
			if (video.getId() == id) {
				try {
					saveSomeVideo(video, videoData);
					return new VideoStatus(VideoStatus.VideoState.READY);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		response.setStatus(404);
		return null;
	}

	@RequestMapping(value = VideoSvcApi.VIDEO_DATA_PATH, method = RequestMethod.GET)
	public @ResponseBody
	void getData(@PathVariable(VideoSvcApi.ID_PARAMETER) long id,
			HttpServletResponse response) {
		for (Video video : this.videos) {
			if (video.getId() == id) {
				try {
					serveSomeVideo(video, response);
					return;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		response.setStatus(404);
	}

	// You would need some Controller method to call this...
	public void saveSomeVideo(Video v, MultipartFile videoData)
			throws IOException {
		videoDataMgr.saveVideoData(v, videoData.getInputStream());
	}

	public void serveSomeVideo(Video v, HttpServletResponse response)
			throws IOException {
		// Of course, you would need to send some headers, etc. to the
		// client too!
		// ...
		videoDataMgr.copyVideoData(v, response.getOutputStream());
	}
}
