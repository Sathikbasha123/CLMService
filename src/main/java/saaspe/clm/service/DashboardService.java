package saaspe.clm.service;

import javax.servlet.http.HttpServletRequest;

import saaspe.clm.model.CommonResponse;

public interface DashboardService {

	CommonResponse getDashboardView(HttpServletRequest request);

	CommonResponse getPcdLoaDashboardCount(HttpServletRequest request);

	CommonResponse getPcdContractDashboardCount(HttpServletRequest request);

	CommonResponse getLoaLatestOnboardedDashboardList(HttpServletRequest request);

	CommonResponse getLoaExpiringDashboardList(HttpServletRequest request);

	CommonResponse getPcdPendingForReviewDashboardList(HttpServletRequest request);

	CommonResponse getContractLatestOnboardedDashboardList(HttpServletRequest request);

	CommonResponse getContractExpiringDashboardList(HttpServletRequest request);

	CommonResponse getCmuLatestOnboardedDashboardList(HttpServletRequest request);

	CommonResponse getCmuExpiringDashboardList(HttpServletRequest request);

	CommonResponse getLooLatestOnboardedDashboardList(HttpServletRequest request);

	CommonResponse getLooExpiringDashboardList(HttpServletRequest request);

	CommonResponse getTaLatestOnboardedDashboardList(HttpServletRequest request);

	CommonResponse getTaExpiringDashboardList(HttpServletRequest request);

	CommonResponse getCommercialPendingForReviewDashboardList(HttpServletRequest request);

	CommonResponse getCommercialCmuDashboardCount(HttpServletRequest request);

	CommonResponse getCommercialLooDashboardCount(HttpServletRequest request);

	CommonResponse getCommercialTaDashboardCount(HttpServletRequest request);

	CommonResponse getLegalContractDashboardCount(HttpServletRequest request,String category,String subsidiary);

	CommonResponse getLegalTaDashboardCount(HttpServletRequest request, String category, String subsidiary);

	CommonResponse getContractCompletedCvefAndStamperDashboardList(HttpServletRequest request, String category, String subsidiary);

	CommonResponse getTaCompletedCvefAndStamperDashboardList(HttpServletRequest request, String category, String subsidiary);
}
