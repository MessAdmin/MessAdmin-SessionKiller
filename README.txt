This MessAdmin plugin corrects a Tomcat bug whereby some expired HttpSession are never invalidated,
by periodically (every 20 minutes) iterating over all HttpSessions and automatically invalidating expired ones.

To install, simply drop MessAdmin-SessionKiller.jar next to MessAdmin.jar or in WEB-INF/lib/.